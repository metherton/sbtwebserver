package martinetherton
import java.nio.file.{Path, Paths}
import java.sql.Timestamp

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.model.{ContentTypes, HttpEntity}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.{PathMatcher, PathMatchers}
import akka.stream.{ActorMaterializer, IOResult}
import akka.stream.scaladsl.{FileIO, Framing, Keep, Sink, Source}
import akka.util.ByteString
import spray.json.DefaultJsonProtocol._
import ch.megard.akka.http.cors.scaladsl.CorsDirectives._
import spray.json.{DeserializationException, JsNumber, JsValue, JsonFormat}

import scala.concurrent.Future
import scala.io.StdIn

object WebServer extends App {

  implicit object TimestampFormat extends JsonFormat[Timestamp] {
    def write(obj: Timestamp) = JsNumber(obj.getTime)

    def read(json: JsValue) = json match {
      case JsNumber(time) => new Timestamp(time.toLong)

      case _ => throw new DeserializationException("Date expected")
    }
  }
  implicit val messageFormat = jsonFormat10(Person)
  implicit val system = ActorSystem("my-system")
  implicit val materializer = ActorMaterializer()
  // needed for the future flatMap/onComplete in the end
  implicit val executionContext = system.dispatcher

  val gedcomFileMap = Map("london1" -> "etherton-london-1.ged")

  def stringArrayFrom(gedcomFile: String): Source[String, Future[IOResult]] = {
    val file: Path = Paths.get(ClassLoader.getSystemResource(gedcomFile).toURI)
    val source: Source[ByteString, Future[IOResult]] = FileIO.fromPath(file)
    val byteStringArray: Source[ByteString, Future[IOResult]] = source.via(Framing.delimiter(
      ByteString("\r\n"), maximumFrameLength = 500, allowTruncation = true))
    val stringArray = byteStringArray.map(_.utf8String)
    stringArray
  }

  def getRequiredLines(stringSource: Source[String, Future[IOResult]]): Source[String, Future[IOResult]] = {
    stringSource.filter(row => List("2 PLAC", "2 DATE", "1 BIRT", "1 SEX", "0 @P", "1 NAME", "1 DEAT", "1 FAMC", "1 FAMS").exists(prefix => row.startsWith(prefix)))
  }

  def listOfPersonStringsFrom(filteredStrings: Source[String, Future[IOResult]]): Source[List[List[String]], Future[IOResult]] = {
    filteredStrings.fold(List(): List[List[String]])((acc: List[List[String]], row: String) => {
      row.startsWith("0 @P") match {
        case true => List(row) :: acc
        case false => acc match {
          case h :: Nil => (row :: h) :: Nil
          case h :: t => (row :: h) :: t
        }
      }
    })
  }

  def personsFrom(personStringArrays: Source[List[List[String]], Future[IOResult]]): Source[List[Person], Future[IOResult]] = {
    personStringArrays.map(a => a.map( arr1 => {
      var arr = arr1.reverse
      val name = arr.find(s => s.startsWith("1 NAME")).getOrElse("1 NAME ").toString.substring(7).split("/")
      val firstName = name(0)
      if (!firstName.equals("Ancestry.com")) {
        val surname = if (name.length > 1) name(1).replace("/", "") else ""
        val indexBirth = arr.indexOf("1 BIRT ");
        var dateOfBirth = "unknown"
        var placeOfBirth = "unknown"
        if (indexBirth >= 0) {
          if (indexBirth + 1 <= arr.size - 1) {
            if (arr(indexBirth + 1).startsWith("2 DATE")) {
              dateOfBirth = arr(indexBirth + 1).toString.substring(7)
            }
          }
          if (indexBirth + 2 <= arr.size - 1) {
            if (arr(indexBirth + 2).startsWith("2 PLAC")) {
              placeOfBirth = arr(indexBirth + 2).toString.substring(7)
            }
          }
        }
        val indexDeath = arr.indexOf("1 DEAT ")
        var dateOfDeath = "unknown"
        var placeOfDeath = "unknown"
        if (indexDeath >= 0) {
          if (indexDeath + 1 <= arr.size - 1) {
            if (arr(indexDeath + 1).startsWith("2 DATE")) {
              dateOfDeath = arr(indexDeath + 1).toString.substring(7)
            }
          }
          if (indexDeath + 2 <= arr.size - 1) {
            if (arr(indexDeath + 2).startsWith("2 PLAC")) {
              placeOfDeath = arr(indexDeath + 2).toString.substring(7)
            }
          }

        }
        val sex = arr.find(s => s.startsWith("1 SEX")).getOrElse("1 SEX ").toString.substring(6)
        val childRelations = arr.filter(a => a.startsWith("1 FAMS")).map(x => x.substring(7).replace("@F", "").replace("@", ""))
        val parentRelation = arr.find(s => s.startsWith("1 FAMC")).getOrElse("1 FAMC ").toString.substring(7).replace("@F", "").replace("@", "")
        val id = arr.find(s => s.startsWith("0 @P")).getOrElse("0 @P").toString.replace("0 @P", "").replace("@ INDI ", "")

        Person(id, firstName, surname, dateOfBirth, placeOfBirth, dateOfDeath, placeOfDeath, sex, childRelations, parentRelation)
      } else {
        Person("UNKNOWN", "UNKNOWN", "UNKNOWN", "UNKNOWN", "UNKNOWN", "UNKNOWN", "UNKNOWN", "UNKNOWN", List("UNKNOWN"), "UNKNOWN")

      }

    }))

  }

  def filteredPersonList(persons: Source[List[Person], Future[IOResult]], firstName: String, surname: String): Source[List[Person], Future[IOResult]] = {
    persons.map(listPersons => listPersons
      .filter(person => (person.firstName.toLowerCase.contains(firstName.toLowerCase) || firstName.equals("*")) &&
        (person.surname.toLowerCase.contains(surname.toLowerCase) || surname.equals("*"))))
  }


  val route1 = cors() {
    path("gedcom" / Segment) { tree =>
      concat(
        get {
          parameters('firstName ? "*", 'surname ? "*") { (firstName, surname) =>
            val sourcePersons: Source[List[Person], Future[IOResult]] = filteredPersonList(personsFrom(listOfPersonStringsFrom(getRequiredLines(stringArrayFrom(gedcomFileMap(tree))))), firstName, surname)
            val sinkPersons: Future[List[Person]] = sourcePersons.runWith(Sink.head)
            complete(sinkPersons)
          }
        },
      )
    }
  }

  val route2 = cors() {
    path("gedcom" / "sussex1") {
      concat(
        get {
          parameters('firstName ? "*", 'surname ? "*") { (firstName, surname) =>
            val sourcePersons: Source[List[Person], Future[IOResult]] = filteredPersonList(personsFrom(listOfPersonStringsFrom(getRequiredLines(stringArrayFrom("etherton-sussex-1.ged")))), firstName, surname)
            val sinkPersons: Future[List[Person]] = sourcePersons.runWith(Sink.head)
            complete(sinkPersons)
          }
        },
      )
    }

  }

  val route3 = cors() {
    path("gedcom" / "london2") {
      concat(
        get {
          parameters('firstName ? "*", 'surname ? "*") { (firstName, surname) =>
            val sourcePersons: Source[List[Person], Future[IOResult]] = filteredPersonList(personsFrom(listOfPersonStringsFrom(getRequiredLines(stringArrayFrom("etherton-london-2.ged")))), firstName, surname)
            val sinkPersons: Future[List[Person]] = sourcePersons.runWith(Sink.head)
            complete(sinkPersons)
          }
        },
      )
    }

  }

  val route4 = cors() {
    path("gedcom" / "sussex2") {
      concat(
        get {
          parameters('firstName ? "*", 'surname ? "*") { (firstName, surname) =>
            val sourcePersons: Source[List[Person], Future[IOResult]] = filteredPersonList(personsFrom(listOfPersonStringsFrom(getRequiredLines(stringArrayFrom("etherton-sussex-2.ged")))), firstName, surname)
            val sinkPersons: Future[List[Person]] = sourcePersons.runWith(Sink.head)
            complete(sinkPersons)
          }
        },
      )
    }

  }

  val route5 = cors() {
    path("gedcom" / "usa1") {
      concat(
        get {
          parameters('firstName ? "*", 'surname ? "*") { (firstName, surname) =>
            val sourcePersons: Source[List[Person], Future[IOResult]] = filteredPersonList(personsFrom(listOfPersonStringsFrom(getRequiredLines(stringArrayFrom("etherton-usa-1.ged")))), firstName, surname)
            val sinkPersons: Future[List[Person]] = sourcePersons.runWith(Sink.head)
            complete(sinkPersons)
          }
        },
      )
    }

  }


  val route6 = cors() {
    path("persons") {
      concat(
        get {
          get {
            complete(HttpEntity(ContentTypes.`text/html(UTF-8)`, "<h1>Say hello to akka-http</h1>"))
          }
        },

      )
    }

  }

  val bindingFuture = Http().bindAndHandle(route1 ~ route2 ~ route3 ~ route4 ~ route5 ~ route6, "0.0.0.0", 8080)

  println(s"Server online at http://localhost:8080/\nPress RETURN to stop...")
  StdIn.readLine() // let it run until user presses return
  bindingFuture
    .flatMap(_.unbind()) // trigger unbinding from the port
    .onComplete(_ => system.terminate()) // and shutdown when done

}