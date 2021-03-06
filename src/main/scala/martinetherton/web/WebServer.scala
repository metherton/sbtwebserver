package martinetherton.web

import java.io.InputStream
import java.security.{KeyStore, SecureRandom}

import akka.actor.{ActorSystem, Props}
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.{RequestContext, Route}
import akka.http.scaladsl.{ConnectionContext, Http, HttpsConnectionContext}
import ch.megard.akka.http.cors.scaladsl.CorsDirectives._
import com.typesafe.akka.extension.quartz.QuartzSchedulerExtension
import javax.net.ssl.{KeyManagerFactory, SSLContext, TrustManagerFactory}
import martinetherton.client.FintechClient.{FindAllLosers, FindAllStocks, FindProfile}
import martinetherton.client.{FintechClient, Request}
import martinetherton.domain.Constants._
import martinetherton.domain._
import martinetherton.mappers.Marshallers
import martinetherton.persistence.{LoserRepository, ProfileRepository}
import spray.json._

import scala.concurrent.duration._
import scala.io.StdIn
import scala.util.{Failure, Success}


object WebServer extends App with Marshallers {

  implicit val system = ActorSystem("martinetherton-webserver")
  implicit val executionContext = system.dispatcher
  val loserRepo = new LoserRepository
  val profileRepo = new ProfileRepository
  val fintechClient = system.actorOf(Props[FintechClient], "FintechClient")
  val scheduler = QuartzSchedulerExtension(system)
//  scheduler.schedule("Every24Hours", fintechClient, FindAllLosers)
//  scheduler.schedule("Every24Hours2", fintechClient, FindAllStocks)
  scheduler.schedule("Every24Hours3", fintechClient, FindProfile)

  val routing: Route = cors() {
    Route.seal {
      get {
        path("losers") {
          val result = loserRepo.getAllLosers()
          complete(result)
        } ~
        path("profiles") {
          val result = profileRepo.getProfiles()
          complete(result)
        } ~
        path("profile" / Segment ) { company =>
          onComplete(Request(Host("fintech"), Url(List("profile", company), Nil)).get) {
            case Success(response) =>
              complete(response)
//              val strictEntityFuture = response.entity.toStrict(10 seconds)
 //             val listProfileFuture = strictEntityFuture.map(_.data.utf8String.parseJson.convertTo[List[Profile]])

   //           onComplete(listProfileFuture) {

//                case Success(listProfile) => complete(listProfile)
//                case Failure(ex) => failWith(ex)
//              }

            case Failure(ex) => failWith(ex)
          }
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
      }
    }
  }

  //val bindingFuture = Http().newServerAt("0.0.0.0", 8443).enableHttps(https).bind(routing)
  val bindingFuture = Http().newServerAt("0.0.0.0", 8080).bind(routing)

  println(s"Server online at http://localhost:8080/\nPress RETURN to stop...")
  StdIn.readLine() // let it run until user presses return
  bindingFuture
    .flatMap(_.unbind()) // trigger unbinding from the port
    .onComplete(_ => system.terminate()) // and shutdown when done


  def https() = {
    val password: Array[Char] = "akka-https".toCharArray // do not store passwords in code, read them from somewhere safe!
    val ks: KeyStore = KeyStore.getInstance("PKCS12")
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

    https
  }
}