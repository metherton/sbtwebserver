package martinetherton.web

import java.io.InputStream
import java.security.{KeyStore, SecureRandom}
import java.sql.Timestamp
import java.util.UUID

import akka.actor.{Actor, ActorLogging, ActorSystem, Props}
import akka.http.scaladsl.model.{DateTime, HttpRequest, HttpResponse, StatusCodes}
import akka.http.scaladsl.model.headers.{HttpCookie, HttpCookiePair, SameSite}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.{AuthenticationFailedRejection, AuthorizationFailedRejection, MethodRejection, MissingCookieRejection, RejectionHandler, Route, ValidationRejection}
import akka.http.scaladsl.server.directives.Credentials
import akka.http.scaladsl.{ConnectionContext, Http, HttpsConnectionContext}
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Sink, Source}
import ch.megard.akka.http.cors.scaladsl.CorsDirectives._
import com.typesafe.akka.extension.quartz.QuartzSchedulerExtension
import javax.net.ssl.{KeyManagerFactory, SSLContext, TrustManagerFactory}
import martinetherton.client.Request
import martinetherton.domain.{CurrencyExchangeRate, Loser, LoserDB, SectorPerformance, Stock, SymbolName, Url, User}
import martinetherton.mappers.Marshallers
import martinetherton.domain.Constants._
import martinetherton.persistence.{LoserRepository, PersonRepository}
import martinetherton.web.FintechDB.{CreateGuitar, FindAllLosers}
import spray.json._

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._
import scala.io.StdIn
import scala.util.{Failure, Success}

case class Guitar(make: String, model: String, quantity: Int = 0)

object FintechDB {
  case class CreateGuitar(guitar: Guitar)
  case class GuitarCreated(id: Int)
  case class FindGuitar(id: Int)
  case object FindAllLosers
  case class AddQuantity(id: Int, quantity: Int)
  case class FindGuitarsInStock(inStock: Boolean)
}

class FintechDB extends Actor with ActorLogging with Marshallers {

  import FintechDB._
  implicit val system = ActorSystem("Fintech")
  implicit val materializer = ActorMaterializer()
  import system.dispatcher


  implicit val loserToLoserDB = (loser: Loser) =>
    LoserDB(None, loser.ticker, loser.changes, loser.price, loser.changesPercentage, loser.companyName, new Timestamp(System.currentTimeMillis()))

  var guitars: Map[Int, Guitar] = Map()
  var currentGuitarId: Int = 0
  val repo = new LoserRepository

  override def receive: Receive = {
    case FindAllLosers =>
      log.info("searching for all losers")
      val result = Request(Host("fintech"), Url(List("losers"), Nil)).get
      result.onComplete {
        case Success(response) => {
          val strictEntityFuture = response.entity.toStrict(10 seconds)
          val losersFuture = strictEntityFuture.map(_.data.utf8String.parseJson.convertTo[List[Loser]])
          losersFuture.onComplete {
            case Success(losers) => {
              val insAct = repo.insert(losers.map(loserToLoserDB))
              insAct.onComplete {
                case Success(result) => println(s"new person added with id: ${result}")
                case Failure(ex) => println(s"could not insert: $ex")
              }
            }
            case Failure(ex) => println(s"Really, I have failed with $ex")
          }
        }
        case Failure(ex) => println(s"I have failed with $ex")
      }
      sender() ! guitars.values.toList
    case FindGuitar(id) =>
      log.info(s"searching guitar by id: $id")
      sender() ! guitars.get(id)
    case CreateGuitar(guitar) =>
      log.info(s"Adding guitar with id $currentGuitarId")
      guitars = guitars + (currentGuitarId -> guitar)
      sender() ! GuitarCreated(currentGuitarId)
      currentGuitarId += 1
    case AddQuantity(id, quantity) =>
      log.info(s"Trying to add $quantity items for guitar $id")
      val guitar: Option[Guitar] = guitars.get(id)
      val newGuitar: Option[Guitar] = guitar.map {
        case Guitar(make, model, q) => Guitar(make, model, q + quantity)
      }
      newGuitar.foreach(guitar => guitars = guitars + (id -> guitar))
      sender() ! newGuitar
    case FindGuitarsInStock(inStock) =>
      log.info(s"searching for all guitars ${if(inStock) "in"  else "out of" } stock")
      if (inStock)
        sender() ! guitars.values.filter(_.quantity > 0)
      else
        sender() ! guitars.values.filter(_.quantity == 0)
  }
}


object WebServer extends App with Marshallers {

  implicit val system = ActorSystem("martinetherton-webserver")
  implicit val executionContext = system.dispatcher

  val repo = new LoserRepository

  val userCredentials = Map("user" -> "password", "user1" -> "password1").withDefaultValue("")
  var userCredentialsStore: scala.collection.mutable.Map[String, String] = scala.collection.mutable.Map[String, String]()
  var sessionIds = scala.collection.mutable.Set[String]()

  def isUserSessionValid(userName: String, sessionId: String): Boolean = (userName, sessionId) match {
    case (uName, sId) => if (userCredentialsStore.contains(uName) && userCredentialsStore(uName).equals(sId)) true else false
    case _ => false
  }

  def isAuthenticated(userNameCookie: Option[HttpCookiePair], sessionIdCookie: Option[HttpCookiePair], xCsrfCookieValueCookie: Option[HttpCookiePair], xCsrfHeaderValue: Option[String]): Boolean = (userNameCookie, sessionIdCookie, xCsrfCookieValueCookie, xCsrfHeaderValue) match {
    case (Some(uName), Some(sId), Some(xc), Some(xh)) => {
      if (isUserSessionValid((uName.value.split("="))(0), (sId.value.split("="))(0)) && (xc.value.split("="))(0).equals(xh)) true else false
    }
    case _ => false
  }

  implicit def myRejectionHandler =
    RejectionHandler.newBuilder()
      .handle {
        case AuthenticationFailedRejection(_, _) =>
          complete(StatusCodes.ImATeapot)
      }
      .handle {
        case MissingCookieRejection(cookieName) =>
          complete(HttpResponse(StatusCodes.BadRequest, entity = "No cookies, no service!!!"))
      }
      .handle {
        case AuthorizationFailedRejection =>
          complete(StatusCodes.Forbidden, "You're out of your depth!")
      }
      .handle {
        case ValidationRejection(msg, _) =>
          complete(StatusCodes.InternalServerError, "That wasn't valid! " + msg)
      }
      .handleAll[MethodRejection] { methodRejections =>
      val names = methodRejections.map(_.supported.name)
      complete(StatusCodes.MethodNotAllowed, s"Can't do that! Supported: ${names mkString " or "}!")
    }
      .handleNotFound { complete((StatusCodes.NotFound, "Not here!")) }
      .result()


  val routing = cors() {

    Route.seal {
      get {
        path("losers") {
          val result = repo.getAllLosers()
          complete(result)
        } ~
        path("tickerSearch" ) {
          parameters('query.as[String], 'limit.as[String], 'exchange.as[String]) { (query, limit, exchange) =>
            onComplete(Request(Host("fintech"), Url(List("search"), List(("query", query), ("limit", limit), ("exchange", exchange)))).get) {
              case Success(response) =>
                val strictEntityFuture = response.entity.toStrict(10 seconds)
                val listTickerSearchFuture = strictEntityFuture.map(_.data.utf8String.parseJson.convertTo[List[SymbolName]])

                onComplete(listTickerSearchFuture) {
                  case Success(listTickerSearch) => complete(listTickerSearch)
                  case Failure(ex) => failWith(ex)
                }

              case Failure(ex) => failWith(ex)
            }
          }
        } ~
        path("currencyExchangeRate") {
          onComplete(Request(Host("fintech"), Url(List("fx"), Nil)).get) {
            case Success(response) =>
              val strictEntityFuture = response.entity.toStrict(10 seconds)
              val listStocksFuture = strictEntityFuture.map(_.data.utf8String.parseJson.convertTo[List[CurrencyExchangeRate]])

              onComplete(listStocksFuture) {
                case Success(listStocks) => complete(listStocks)
                case Failure(ex) => failWith(ex)
              }
            case Failure(ex) => failWith(ex)
          }
        } ~
        path("sectorsPerformance") {
          onComplete(Request(Host("fintech"), Url(List("stock", "sectors-performance"), Nil)).get) {
            case Success(response) =>
              val strictEntityFuture = response.entity.toStrict(10 seconds)
              val listStocksFuture = strictEntityFuture.map(_.data.utf8String.parseJson.convertTo[SectorPerformance])

              onComplete(listStocksFuture) {
                case Success(listStocks) => complete(listStocks)
                case Failure(ex) => failWith(ex)
              }
            case Failure(ex) => failWith(ex)
          }
        } ~
        path("liststocks") {
          onComplete(Request(Host("fintech"), Url(List("stock", "list"), Nil)).get) {
            case Success(response) =>
              val strictEntityFuture = response.entity.toStrict(10 seconds)
              val listStocksFuture = strictEntityFuture.map(_.data.utf8String.parseJson.convertTo[List[Stock]])

              onComplete(listStocksFuture) {
                case Success(listStocks) => complete(listStocks)
                case Failure(ex) => failWith(ex)
              }

            case Failure(ex) => failWith(ex)
          }
        } ~
        pathEndOrSingleSlash {
          complete(StatusCodes.OK)
        }
      } ~
      post {
        path("login") {
          extractCredentials { credentials =>
            authenticateBasic(realm = "sharesite", myUserPassAuthenticator) { authenticationDetails =>
              setCookie(
                HttpCookie("sessionid", authenticationDetails._1).withSameSite(SameSite.None).withSecure(true).withExpires(DateTime.MaxValue),
                HttpCookie("username", authenticationDetails._2).withSameSite(SameSite.None).withSecure(true).withExpires(DateTime.MaxValue),
                HttpCookie("x-csrf-token", authenticationDetails._3).withSameSite(SameSite.None).withSecure(true).withExpires(DateTime.now.plus(28800000))
              ) {
                complete(User(authenticationDetails._2, authenticationDetails._1, authenticationDetails._3))
              }
            }
          }
        } ~
        path("logout") {
          deleteCookie(
            HttpCookie("sessionid", "").withSameSite(SameSite.None).withSecure(true),
            HttpCookie ("username", "").withSameSite(SameSite.None).withSecure(true),
            HttpCookie ("x-csrf-token", "").withSameSite(SameSite.None).withSecure(true)
          ) {
            complete("The user has logged out")
          }
        } ~
        path("losers") {
          optionalCookie("x-csrf-token") { xCsrfCookieToken =>
            optionalCookie("username") { userName =>
              optionalCookie("sessionid") { sessionId =>
                formField("xCsrfToken".optional) { xCsrfToken =>
                  if (isAuthenticated(userName, sessionId, xCsrfCookieToken, xCsrfToken)) {
                    onComplete(Request(Host("fintech"), Url(List("losers"), Nil)).get) {
                      case Success(response) =>
                        val strictEntityFuture = response.entity.toStrict(10 seconds)
                        val listStocksFuture = strictEntityFuture.map(_.data.utf8String.parseJson.convertTo[List[Loser]])

                        onComplete(listStocksFuture) {
                          case Success(listStocks) => complete(listStocks)
                          case Failure(ex) => failWith(ex)
                        }
                      case Failure(ex) => failWith(ex)
                    }
                  } else {
                    complete(StatusCodes.Unauthorized)
                  }
                }
              }
            }
          }
        }
      }
    }
  }

//  val password: Array[Char] = "change me".toCharArray // do not store passwords in code, read them from somewhere safe!
  val password: Array[Char] = "akka-https".toCharArray // do not store passwords in code, read them from somewhere safe!

  val ks: KeyStore = KeyStore.getInstance("PKCS12")
//  val keystore: InputStream = getClass.getClassLoader.getResourceAsStream("server.p12")

  val keystore: InputStream = getClass.getClassLoader.getResourceAsStream("keystore.pkcs12")

  require(keystore != null, "Keystore required!")
  ks.load(keystore, password)

  val keyManagerFactory: KeyManagerFactory = KeyManagerFactory.getInstance("SunX509")
  keyManagerFactory.init(ks, password)

  val tmf: TrustManagerFactory = TrustManagerFactory.getInstance("SunX509")
  tmf.init(ks)

  val sslContext: SSLContext = SSLContext.getInstance("TLS")
  sslContext.init(keyManagerFactory.getKeyManagers, tmf.getTrustManagers, new SecureRandom)
  val https: HttpsConnectionContext = ConnectionContext.httpsServer(sslContext)

  /*

      set up
   */
  val guitarDb = system.actorOf(Props[FintechDB], "LowLevelGuitarDB")
  val guitarList = List(
    Guitar("Fender", "Stratocaster"),
    Guitar("Gibson", "Les Paul"),
    Guitar("Martin", "LX1")
  )
  guitarList.foreach { guitar =>
    guitarDb ! CreateGuitar(guitar)
  }

  QuartzSchedulerExtension(system).schedule("Every24Hours", guitarDb, FindAllLosers)

  //val bindingFuture = Http().newServerAt("0.0.0.0", 8443).enableHttps(https).bind(routing)
  val bindingFuture = Http().newServerAt("0.0.0.0", 8080).bind(routing)

  println(s"Server online at http://localhost:8080/\nPress RETURN to stop...")
  StdIn.readLine() // let it run until user presses return
  bindingFuture
    .flatMap(_.unbind()) // trigger unbinding from the port
    .onComplete(_ => system.terminate()) // and shutdown when done


  def myUserPassAuthenticator(credentials: Credentials): Option[(String, String, String)] =
    credentials match {
      case p @ Credentials.Provided(userName) if p.verify(userCredentials(userName)) => {
        val sessionId = UUID.randomUUID.toString
        val xCsrfToken = UUID.randomUUID.toString
        if (userCredentialsStore.contains(userName)) {
          userCredentialsStore(userName) = sessionId
        } else {
          userCredentialsStore += userName -> sessionId
        }
        Some(sessionId, userName, xCsrfToken)
      }
      case _ => None
    }

}