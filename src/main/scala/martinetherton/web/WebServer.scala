package martinetherton.web

import java.io.InputStream
import java.security.{KeyStore, SecureRandom}
import java.util.UUID

import akka.actor.ActorSystem
import akka.http.scaladsl.model.{DateTime, HttpHeader, HttpRequest, StatusCodes}
import akka.http.scaladsl.model.headers.{HttpCookie, HttpCookiePair, HttpOrigin, Origin, RawHeader, SameSite}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.{Directive1, Route}
import akka.http.scaladsl.server.directives.Credentials
import akka.http.scaladsl.{ConnectionContext, Http, HttpsConnectionContext}
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Sink, Source}
import ch.megard.akka.http.cors.scaladsl.CorsDirectives._
import javax.net.ssl.{KeyManagerFactory, SSLContext, TrustManagerFactory}
import martinetherton.client.Request
import martinetherton.domain.{Constants, CurrencyExchangeRate, Executive, Loser, Resource, SectorChange, SectorPerformance, Stock, SymbolName, Url, User}
import martinetherton.mappers.Marshallers
import martinetherton.domain.Constants._
import martinetherton.web.WebServer.myUserPassAuthenticator
import spray.json._

import scala.concurrent.duration._
import scala.io.StdIn
import scala.util.{Failure, Success}

object WebServer extends App with Marshallers {

  implicit val system = ActorSystem("martinetherton-webserver")
  implicit val executionContext = system.dispatcher

  val userCredentials = Map("user" -> "password", "user1" -> "password1")
  var userCredentialsStore: scala.collection.mutable.Map[String, String] = scala.collection.mutable.Map[String, String]()
  var sessionIds = scala.collection.mutable.Set[String]()

  def isUserSessionValid(userName: String, sessionId: String): Boolean = (userName, sessionId) match {
    case (uName, sId) => if (userCredentialsStore.contains(uName) && userCredentialsStore(uName).equals(sId)) true else false
    case _ => false
  }

  def isAuthenticated(userNameCookie: Option[HttpCookiePair], sessionIdCookie: Option[HttpCookiePair], xsrfCookieValueCookie: Option[HttpCookiePair], xsrfHeaderValue: Option[String]): Boolean = (userNameCookie, sessionIdCookie, xsrfCookieValueCookie, xsrfHeaderValue) match {
    case (Some(uName), Some(sId), Some(xc), Some(xh)) => {
      if (isUserSessionValid((uName.value.split("="))(0), (sId.value.split("="))(0)) && (xc.value.split("="))(0).equals(xh)) true else false
//      if (isUserSessionValid((uName.value.split("="))(0), (sId.value.split("="))(0)) ) true else false
    }
    case _ => false
  }

//  def isAuthenticated(userName: Option[String], sessionId: Option[String], xsrfCookieValue: Option[String], xsrfHeaderValue: Option[String]): Boolean = (userName, sessionId, xsrfCookieValue, xsrfHeaderValue) match {
//    case (Some(uName), Some(sId), Some(xc), Some(xh)) => {
//      if (isUserSessionValid(uName, sId) && xsrfCookieValue.equals(xsrfHeaderValue)) true else false
//    }
//    case _ => false
//  }

//  def isAuthenticated(userName: Option[HttpCookiePair]): Boolean = userName match {
//    case Some(un) => un.
//    case _ => false
//  }

  val routing = cors() {

    Route.seal {
      path("login") {
        extractCredentials { credentials =>
          authenticateBasic(realm = "secure site", myUserPassAuthenticator) { authenticationDetails =>
            respondWithHeaders(RawHeader("x-csrf-token", authenticationDetails._3)) {
              setCookie(HttpCookie("sessionid", authenticationDetails._1).withSameSite(SameSite.None).withSecure(true), HttpCookie("username", authenticationDetails._2).withSameSite(SameSite.None).withSecure(true), HttpCookie("x-csrf-token", authenticationDetails._3).withSameSite(SameSite.None).withSecure(true)) {
                complete(User(authenticationDetails._2, authenticationDetails._1, authenticationDetails._3))

              }
            }
          }
        }
      } ~
//      path("secured") {
//        extractCredentials { creds =>
//          authenticateBasic(realm = "secure site", myUserPassAuthenticator) { userName =>
//            complete(s"The user is '$userName'")
//          }
//        }
//      } ~
      path("tickerSearch" ) {
        get {
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
        }
      } ~
      path("currencyExchangeRate") {
        get {
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
        }
      } ~
      path("sectorsPerformance") {
        get {
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
        }
      } ~
      path("losers") {
        optionalCookie("x-csrf-token") { xsrfCookieToken =>
          optionalCookie("username") { userName =>
            optionalCookie("sessionid") { sessionId =>
              optionalHeaderValueByName("x-csrf-token") { xsrfHeaderValue =>
                if (isAuthenticated(userName, sessionId, xsrfCookieToken, xsrfHeaderValue)) {
                  get {
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
                  }
                } else {
                  complete(StatusCodes.Unauthorized)
                }

              }
            }
          }

        }
      } ~
      path("liststocks") {
        get {
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

  //val bindingFuture = Http().newServerAt("localhost", 8443).enableHttps(https).bind(routing)
  //val bindingFuture1 = Http().newServerAt("localhost", 8080).bind(routing)
  val bindingFuture = Http().newServerAt("0.0.0.0", 8443).enableHttps(https).bind(routing)
  val bindingFuture1 = Http().newServerAt("0.0.0.0", 8080).bind(routing)

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

//  def myUserPassAuthenticator(credentials: Credentials): (Option[String], Option[String], Option[String])  =
//    credentials match {
//      case p @ Credentials.Provided(userName) if p.verify(userCredentials(userName)) => {
//        val sessionId = UUID.randomUUID.toString
//        val xsrfToken = UUID.randomUUID.toString
//        if (userCredentialsStore.contains(userName)) {
//          userCredentialsStore(userName) = sessionId
//        } else {
//          userCredentialsStore += userName -> sessionId
//        }
//        (Some(userName), Some(sessionId), Some(xsrfToken))
//      }
//      case _ => (None, None, None)
//    }


}