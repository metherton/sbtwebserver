package martinetherton
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

object WebServer extends App {

  import slick.jdbc.H2Profile.api._

  implicit val messageFormat = jsonFormat3(Person)

  implicit val system = ActorSystem("my-system")
  implicit val materializer = ActorMaterializer()
  // needed for the future flatMap/onComplete in the end
  implicit val executionContext = system.dispatcher

  val repo = new PersonRepository

//  val route =
//    path("items") {
//      concat(
//        get {
//          val messagesAction: DBIO[Seq[Message]] = messages.result
//          val messagesFuture: Future[Seq[Message]] = db.run(messagesAction)
//
//     //     val messagesResults = result(messagesFuture, 2.seconds)
////          val sql = messages.result.statements.mkString
//          complete(messagesFuture)
//
//        },
//        post {
//          def newMessage = Seq(
//            Message("Martin", "This is my new message")
//          )
//          val ins: DBIO[Option[Int]] = messages ++= newMessage
//          val insAct: Future[Option[Int]] = db.run(ins)
//          onComplete(insAct) { done =>
//            complete("new message added")
//          }
//        }
//      )
//    }

  val route1 =
    path("persons") {
      concat(
        get {

          import martinetherton.PersonRepository
          //     val messagesResults = result(messagesFuture, 2.seconds)
          //          val sql = messages.result.statements.mkString

          val result = repo.getPersons
          complete(result)

        },
        post {
          entity(as[Person]) { person =>
   //         def newMessage = Seq(
   //           Message("Martin", "This is my new message")
   //         )

            val insAct = repo.insert(person)
            onComplete(insAct) { done =>
              complete(s"new person added with id: ${insAct}")
            }
          }
        }
      )
    }

  val bindingFuture = Http().bindAndHandle(route1, "localhost", 8080)

  println(s"Server online at http://localhost:8080/\nPress RETURN to stop...")
  StdIn.readLine() // let it run until user presses return
  bindingFuture
    .flatMap(_.unbind()) // trigger unbinding from the port
    .onComplete(_ => system.terminate()) // and shutdown when done

}