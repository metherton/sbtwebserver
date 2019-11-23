package martinetherton

import akka.actor.ActorSystem
import akka.stream.scaladsl._
import akka.util.ByteString
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{ HttpEntity, ContentTypes }
import akka.http.scaladsl.server.Directives._
import akka.stream.ActorMaterializer
import scala.util.Random
import scala.io.StdIn

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.stream.ActorMaterializer
import akka.Done
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.model.StatusCodes
// for JSON serialization/deserialization following dependency is required:
// "com.typesafe.akka" %% "akka-http-spray-json" % "10.1.7"
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import spray.json.DefaultJsonProtocol._

import scala.io.StdIn

import scala.concurrent.Future

import akka.actor.{ Actor, ActorSystem, Props, ActorLogging }
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.pattern.ask
import akka.stream.ActorMaterializer
import akka.util.Timeout
import spray.json.DefaultJsonProtocol._
import scala.concurrent.Future
import scala.concurrent.duration._
import scala.io.StdIn

import ch.megard.akka.http.cors.scaladsl.CorsDirectives._

object WebServer {

  case class Bid(userId: String, offer: Int)
  case object GetBids
  case class Bids(bids: List[Bid])

  class Auction extends Actor with ActorLogging {
    var bids = List.empty[Bid]
    def receive = {
      case bid @ Bid(userId, offer) =>
        bids = bids :+ bid
        log.info(s"Bid complete: $userId, $offer")
      case GetBids => sender() ! Bids(bids)
      case _       => log.info("Invalid message")
    }
  }

  // these are from spray-json
  implicit val bidFormat = jsonFormat2(Bid)
  implicit val bidsFormat = jsonFormat1(Bids)

  def main(args: Array[String]) {
    implicit val system = ActorSystem()
    implicit val materializer = ActorMaterializer()
    // needed for the future flatMap/onComplete in the end
    implicit val executionContext = system.dispatcher

    val auction = system.actorOf(Props[Auction], "auction")

    val route = cors() {
      path("auction") {
        concat(
          put {
            parameter('bid.as[Int], 'user ? "marty") { (bid, user) =>
              // place a bid, fire-and-forget
              auction ! Bid(user, bid)
              complete((StatusCodes.Accepted, "bid placed"))
            }
          },
          get {
            implicit val timeout: Timeout = 5.seconds

            // query the actor for the current auction state
            val bids: Future[Bids] = (auction ? GetBids).mapTo[Bids]
            complete(bids)
          }
        )
      }

    }

    val bindingFuture = Http().bindAndHandle(route, "141.138.139.81", 8080)
    println(s"Server online at http://141.138.139.81:8080/\nPress RETURN to stop...")
    StdIn.readLine() // let it run until user presses return
    bindingFuture
      .flatMap(_.unbind()) // trigger unbinding from the port
      .onComplete(_ => system.terminate()) // and shutdown when done

  }
}