package martinetherton
// for JSON serialization/deserialization following dependency is required:
// "com.typesafe.akka" %% "akka-http-spray-json" % "10.1.7"
import akka.Done
import akka.http.scaladsl.model.StatusCodes
import spray.json.DefaultJsonProtocol.{jsonFormat1, jsonFormat2}

import scala.concurrent.Future
// for JSON serialization/deserialization following dependency is required:
// "com.typesafe.akka" %% "akka-http-spray-json" % "10.1.7"
// for JSON serialization/deserialization following dependency is required:
// "com.typesafe.akka" %% "akka-http-spray-json" % "10.1.7"
import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.server.Directives._
import akka.stream.ActorMaterializer
import spray.json.DefaultJsonProtocol._

import scala.io.StdIn

//#imports
// Use H2Profile to connect to an H2 database
//#imports

//#imports
// Use H2Profile to connect to an H2 database
//#imports

//#imports
// Use H2Profile to connect to an H2 database
//#imports

object WebServer extends App {

  import Marshallers._

  implicit val system = ActorSystem("my-system")
  implicit val materializer = ActorMaterializer()
  // needed for the future flatMap/onComplete in the end
  implicit val executionContext = system.dispatcher

  val sm = SystemModule

  val route =
    path("items") {
      concat(
        get {
          val result = sm.itemRepo.findAllItems()
          complete(result)
        },
        post {
          val result = sm.itemRepo.saveItem(ItemVo(4L, "newone"))
          onComplete(result) { done =>
            complete("order created")
          }
        }
      )
    }

  val bindingFuture = Http().bindAndHandle(route, "localhost", 8080)

  println(s"Server online at http://localhost:8080/\nPress RETURN to stop...")
  StdIn.readLine() // let it run until user presses return
  bindingFuture
    .flatMap(_.unbind()) // trigger unbinding from the port
    .onComplete(_ => system.terminate()) // and shutdown when done

}