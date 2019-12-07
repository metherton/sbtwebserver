package martinetherton


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

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer




object WebServer extends App {

  import slick.jdbc.H2Profile.api._
  case class Message(sender: String, content: String, id: Long = 0L)
  implicit val messageFormat = jsonFormat3(Message)

  class MessageTable(tag: Tag) extends Table[Message](tag, "message") {
    def id = column[Long]("id", O.PrimaryKey, O.AutoInc)
    def sender = column[String]("sender")
    def content = column[String]("content")
    def * = (sender, content, id).mapTo[Message]
  }

  val messages = TableQuery[MessageTable]
  val halSays = messages.filter(_.sender === "HAL")

  val db = Database.forConfig("chapter01")

  messages.schema.createStatements.mkString
  val action: DBIO[Unit] = messages.schema.create

  import scala.concurrent.Future
  val future: Future[Unit] = db.run(action)
  import scala.concurrent.Await
  import scala.concurrent.duration._
  val result = Await.result(future, 2.seconds)

  def freshTestData = Seq(
    Message("Dave", "Hello, HAL. Do you read me, HAL?"),
    Message("HAL", "Affirmative, Dave. I read you."),
    Message("Dave", "Open the pod bay doors, HAL."),
    Message("HAL", "I'm sorry, Dave. I'm afraid I can't do that.")
  )

  val insert: DBIO[Option[Int]] = messages ++= freshTestData
  val insertAction: Future[Option[Int]] = db.run(insert)

  val rowCount = Await.result(insertAction, 2.seconds)

  implicit val system = ActorSystem("my-system")
  implicit val materializer = ActorMaterializer()
  // needed for the future flatMap/onComplete in the end
  implicit val executionContext = system.dispatcher


  val route =
    path("items") {
      concat(
        get {
          val messagesAction: DBIO[Seq[Message]] = messages.result
          val messagesFuture: Future[Seq[Message]] = db.run(messagesAction)

     //     val messagesResults = result(messagesFuture, 2.seconds)
//          val sql = messages.result.statements.mkString
          complete(messagesFuture)

        },
        post {
          def newMessage = Seq(
            Message("Martin", "This is my new message")
          )
          val ins: DBIO[Option[Int]] = messages ++= newMessage
          val insAct: Future[Option[Int]] = db.run(ins)
          onComplete(insAct) { done =>
            complete("new message added")
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