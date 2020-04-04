package martinetherton
// for JSON serialization/deserialization following dependency is required:
// "com.typesafe.akka" %% "akka-http-spray-json" % "10.1.7"
// for JSON serialization/deserialization following dependency is required:
// "com.typesafe.akka" %% "akka-http-spray-json" % "10.1.7"
import java.nio.file.Paths

import akka.actor.ActorSystem
import akka.stream.IOResult
import akka.stream.scaladsl.{FileIO, Source}
import akka.util.ByteString
import akka.{Done, NotUsed}

import scala.concurrent.Future

object WebServer extends App {

//  import slick.jdbc.H2Profile.api._
//
//  implicit object TimestampFormat extends JsonFormat[Timestamp] {
//    def write(obj: Timestamp) = JsNumber(obj.getTime)
//
//    def read(json: JsValue) = json match {
//      case JsNumber(time) => new Timestamp(time.toLong)
//
//      case _ => throw new DeserializationException("Date expected")
//    }
//  }
//  implicit val messageFormat = jsonFormat7(Person)
//
//  implicit val system = ActorSystem("my-system")
//  implicit val materializer = ActorMaterializer()
//  // needed for the future flatMap/onComplete in the end
//  implicit val executionContext = system.dispatcher


  implicit val system = ActorSystem("QuickStart")
  val source: Source[Int, NotUsed] = Source(1 to 100)
  source.runForeach(i => println(i))

  val factorials = source.scan(BigInt(1))((acc, next) => acc * next)

  val result: Future[IOResult] =
    factorials.map(num => ByteString(s"$num\n")).runWith(FileIO.toPath(Paths.get("factorials.txt")))


  val done: Future[Done] = source.runForeach(i => println(i))

  implicit val ec = system.dispatcher
  done.onComplete(_ => system.terminate())

//  val repo = new PersonRepository

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

//  val route1 = cors() {
//    path("persons") {
//      concat(
//        get {
//
//          import martinetherton.PersonRepository
//          //     val messagesResults = result(messagesFuture, 2.seconds)
//          //          val sql = messages.result.statements.mkString
//
//          val result = repo.getPersons
//          complete(result)
//
//        },
//        post {
//          entity(as[Person]) { person =>
//            //         def newMessage = Seq(
//            //           Message("Martin", "This is my new message")
//            //         )
//
//            val insAct = repo.insert(person)
//            onComplete(insAct) { done =>
//              complete(s"new person added with id: ${insAct}")
//            }
//          }
//        }
//      )
//    }
//
//  }
//
//  val bindingFuture = Http().bindAndHandle(route1, "0.0.0.0", 8080)
//
//  println(s"Server online at http://localhost:8080/\nPress RETURN to stop...")
//  StdIn.readLine() // let it run until user presses return
//  bindingFuture
//    .flatMap(_.unbind()) // trigger unbinding from the port
//    .onComplete(_ => system.terminate()) // and shutdown when done

}