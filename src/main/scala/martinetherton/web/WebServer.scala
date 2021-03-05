package martinetherton.web

import akka.actor.ActorSystem
import akka.http.scaladsl.model.HttpRequest
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.{ConnectionContext, Http}
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Sink, Source}
import ch.megard.akka.http.cors.scaladsl.CorsDirectives._
import javax.net.ssl.SSLContext
import martinetherton.client.Request
import martinetherton.domain.{Constants, Executive, Resource, Stock, SymbolName, Url}
import martinetherton.mappers.Marshallers
import martinetherton.domain.Constants._
import spray.json._

import scala.concurrent.duration._
import scala.io.StdIn
import scala.util.{Failure, Success}

object WebServer extends App with Marshallers {

  implicit val system = ActorSystem("martinetherton-webserver")
  implicit val executionContext = system.dispatcher

  val routing = cors() {
    path("tickerSearch" ) {
      parameters('query.as[String], 'limit.as[String], 'exchange.as[String]) { (query, limit, exchange) =>
        get {
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

  val bindingFuture = Http().bindAndHandle(routing, "0.0.0.0", 8080)

  println(s"Server online at http://localhost:8080/\nPress RETURN to stop...")
  StdIn.readLine() // let it run until user presses return
  bindingFuture
    .flatMap(_.unbind()) // trigger unbinding from the port
    .onComplete(_ => system.terminate()) // and shutdown when done

}