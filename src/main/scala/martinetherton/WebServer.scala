package martinetherton
// for JSON serialization/deserialization following dependency is required:
// "com.typesafe.akka" %% "akka-http-spray-json" % "10.1.7"
// for JSON serialization/deserialization following dependency is required:
// "com.typesafe.akka" %% "akka-http-spray-json" % "10.1.7"
import java.sql.Timestamp

import akka.actor.ActorSystem
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.{ConnectionContext, Http}
import akka.http.scaladsl.model.{HttpRequest, StatusCodes}
import akka.http.scaladsl.server.Directives._
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Sink, Source}
import ch.megard.akka.http.cors.scaladsl.CorsDirectives._
import javax.net.ssl.SSLContext
import spray.json._
import scala.concurrent.duration._

import scala.io.StdIn
import scala.util.{Failure, Success}

case class Stock(symbol: String, price: Double)
trait StockJsonProtocol extends DefaultJsonProtocol {
  implicit val stockFormat = jsonFormat2(Stock)

}
object WebServer extends App with StockJsonProtocol with SprayJsonSupport {

  implicit val system = ActorSystem("my-system")
  implicit val materializer = ActorMaterializer()
  // needed for the future flatMap/onComplete in the end
  implicit val executionContext = system.dispatcher

  val route1 = cors() {
    path("liststocks") {
      get {
        val httpsConnectionContext = ConnectionContext.https(SSLContext.getDefault)
        val connectionFlow = Http().outgoingConnectionHttps("financialmodelingprep.com", 443, httpsConnectionContext)


        def oneOffRequest(request: HttpRequest) =
          Source.single(request).via(connectionFlow).runWith(Sink.head)

        onComplete(oneOffRequest(HttpRequest(uri = "/api/v3/stock/list?apikey=0a314c85fe75ed860b38f1d1b4c2bdd2"))) {
          case Success(response) =>
            val strictEntityFuture = response.entity.toStrict(10 seconds)
            val stocksFuture = strictEntityFuture.map(_.data.utf8String.parseJson.convertTo[List[Stock]])

            onComplete(stocksFuture) {
              case Success(x) => complete(x)
              case Failure(ex) => failWith(ex)
            }

          case Failure(ex) => failWith(ex)
        }
      }
    }

  }

  val bindingFuture = Http().bindAndHandle(route1, "0.0.0.0", 8080)

  println(s"Server online at http://localhost:8080/\nPress RETURN to stop...")
  StdIn.readLine() // let it run until user presses return
  bindingFuture
    .flatMap(_.unbind()) // trigger unbinding from the port
    .onComplete(_ => system.terminate()) // and shutdown when done

}