package martinetherton
// for JSON serialization/deserialization following dependency is required:
// "com.typesafe.akka" %% "akka-http-spray-json" % "10.1.7"
// for JSON serialization/deserialization following dependency is required:
// "com.typesafe.akka" %% "akka-http-spray-json" % "10.1.7"
import java.nio.file.Paths
import java.sql.Timestamp

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.server.Directives._
import akka.stream.{ActorMaterializer, IOResult}
import akka.stream.scaladsl.{FileIO, Framing, Keep, Sink, Source}
import akka.util.ByteString
import spray.json.DefaultJsonProtocol._
import ch.megard.akka.http.cors.scaladsl.CorsDirectives._
import spray.json.{DeserializationException, JsNumber, JsValue, JsonFormat}

import scala.concurrent.Future
import scala.io.StdIn

object WebServer extends App {

  import slick.jdbc.H2Profile.api._

  implicit object TimestampFormat extends JsonFormat[Timestamp] {
    def write(obj: Timestamp) = JsNumber(obj.getTime)

    def read(json: JsValue) = json match {
      case JsNumber(time) => new Timestamp(time.toLong)

      case _ => throw new DeserializationException("Date expected")
    }
  }
// implicit val messageFormat = jsonFormat7(Person)
  implicit val messageFormat = jsonFormat4(Person)
 // implicit val messageFormat = jsonFormat3(Person)

  implicit val system = ActorSystem("my-system")
  implicit val materializer = ActorMaterializer()
  // needed for the future flatMap/onComplete in the end
  implicit val executionContext = system.dispatcher

 // val repo = new PersonRepository

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

//  val file = Paths.get("/Users/martin/myprojects/sbt/mywebserver/server/src/main/scala/martinetherton/etherton-london-1.ged")
  val file = Paths.get(ClassLoader.getSystemResource("etherton-london-1.ged").toURI)

  val source: Source[ByteString, Future[IOResult]] = FileIO.fromPath(file)

//  val linesStream = source.via(Framing.delimiter(ByteString("0 @"), maximumFrameLength = 64000, allowTruncation = true)).via(Framing.delimiter(
//    ByteString("\r\n"), maximumFrameLength = 500, allowTruncation = true))
//    .map(_.utf8String).filter(row => row.startsWith("2 PLAC") ||row.startsWith("2 DATE") || row.startsWith("1 BIRT") || row.startsWith("1 SEX") || row.startsWith("0 @") || row.startsWith("1 NAME")).runWith(Sink.seq)

//  case class Person(name: String, birthDate: String)


//  val persons = List("0 INDI 7", "1 NAME martin etherton", "2 BIRTH 4th March 1963", "0 INDI 8", "1 NAME sidney etherot")
//
//  val folded: List[List[String]] = persons.foldLeft(List(): List[List[String]])((acc: List[List[String]], row: String) => {
//    row.startsWith("0 INDI") match {
//      case true => List(row) :: acc
//      case false => acc match {
//        case h :: Nil => (row :: h) :: Nil
//        case h :: t => (row :: h) :: t
//      }
//    }
//  })

//  val mappedPersons = folded.map(
//    (arr) => Person(
//      arr.find((s => s.startsWith("1 NAME"))).getOrElse("1 NAME ").substring(7),
//      arr.find(s => s.startsWith("2 BIRTH")).getOrElse("2 BIRTH ").substring(8))
//  )


  //val sink

  val arrayStrings = source.via(Framing.delimiter(
    ByteString("\r\n"), maximumFrameLength = 500, allowTruncation = true))
    .map(_.utf8String).filter(row => row.startsWith("2 PLAC") ||row.startsWith("2 DATE") || row.startsWith("1 BIRT") || row.startsWith("1 SEX") || row.startsWith("0 @P") || row.startsWith("1 NAME"))
    .fold(List(): List[List[String]])((acc: List[List[String]], row: String) => {
      row.startsWith("0 @P") match {
        case true => List(row) :: acc
        case false => acc match {
          case h :: Nil => (row :: h) :: Nil
          case h :: t => (row :: h) :: t
        }
      }
    })
  val persons: Source[List[Person], Future[IOResult]] = arrayStrings.map(a => a.map( arr => {
    val name = arr.find(s => s.startsWith("1 NAME")).getOrElse("1 NAME ").toString.substring(7).split("/")
    val firstName = name(0)
    val surname = if (name.length > 1) name(1).replace("/", "") else ""
    val dateOfBirth = arr.find(s => s.startsWith("2 DATE")).getOrElse("2 DATE ").toString.substring(7)
    val placeOfBirth = arr.find(s => s.startsWith("2 PLAC")).getOrElse("2 PLAC ").toString.substring(7)
    Person(firstName, surname, dateOfBirth, placeOfBirth)
  }))




//    .map(
//      (arr) => Person(
//        arr.find(s => s.startsWith("1 NAME")).getOrElse("1 NAME "),
//        arr.find(s => s.startsWith("2 DATE")).getOrElse("2 DATE "),
//        arr.find(s => s.startsWith("2 PLAC")).getOrElse("2 PLAC ")
//      )
//    )

   // .runWith(Sink.seq)


 // val future = source.map(bs => bs.utf8String).runWith(Sink.seq)

//  val persons = Future{List(Person("martin", "etherton", new Timestamp(12121), "high street", "sheffield", "england"))}

  val route1 = cors() {
    path("persons") {
      concat(
        get {
          parameters('firstName ? "*", 'surname ? "*") { (firstName, surname) =>
            //     import martinetherton.PersonRepository
            //     val messagesResults = result(messagesFuture, 2.seconds)
            //          val sql = messages.result.statements.mkString

            // val result = repo.getPersons
            val filteredPersons = persons.map(listPersons => listPersons
              .filter(person => (person.firstName.toLowerCase.contains(firstName.toLowerCase) || firstName.equals("*")) &&
                (person.surname.toLowerCase.contains(surname.toLowerCase) || surname.equals("*"))))

            val sinkPersons = filteredPersons.runWith(Sink.seq)

            complete(sinkPersons)
          }
        },
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
      )
    }

  }

  val bindingFuture = Http().bindAndHandle(route1, "0.0.0.0", 8080)

  println(s"Server online at http://localhost:8080/\nPress RETURN to stop...")
  StdIn.readLine() // let it run until user presses return
  bindingFuture
    .flatMap(_.unbind()) // trigger unbinding from the port
    .onComplete(_ => system.terminate()) // and shutdown when done

}