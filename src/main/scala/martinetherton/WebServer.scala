package martinetherton
import java.nio.file.{Path, Paths}
import java.sql.Timestamp
import java.time.LocalDateTime

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.server.Directives._
import akka.stream.scaladsl.{FileIO, Flow, Framing, Keep, Sink, Source}
import akka.stream.{ActorMaterializer, IOResult}
import akka.util.ByteString
import ch.megard.akka.http.cors.scaladsl.CorsDirectives._
import spray.json.DefaultJsonProtocol._
import spray.json.{DeserializationException, JsNumber, JsValue, JsonFormat}

import scala.concurrent.{Await, Future}
import scala.concurrent.duration._
import scala.io.StdIn
import scala.util.Success

object WebServer extends App {

  implicit object TimestampFormat extends JsonFormat[Timestamp] {
    def write(obj: Timestamp) = JsNumber(obj.getTime)

    def read(json: JsValue) = json match {
      case JsNumber(time) => new Timestamp(time.toLong)

      case _ => throw new DeserializationException("Date expected")
    }
  }
  implicit val gedcomPersonFormat = jsonFormat10(GedcomPerson)
  implicit val personFormat = jsonFormat13(Person)
  implicit val system = ActorSystem("my-system")
  implicit val materializer = ActorMaterializer()
  // needed for the future flatMap/onComplete in the end
  implicit val executionContext = system.dispatcher

  val gedcomFileMap =
    Map("london1" -> "etherton-london-1.ged",
      "sussex1" -> "etherton-sussex-1.ged",
      "london2" -> "etherton-london-2.ged",
      "sussex2" -> "etherton-sussex-2.ged",
      "usa1" -> "etherton-usa-1.ged")


  val gedcom = cors() {
    path("gedcom" / Segment) { tree =>
      concat(
        get {
          parameters('firstName ? "*", 'surname ? "*") { (firstName, surname) =>
            val sourcePersons: Source[List[GedcomPerson], Future[IOResult]] = filteredPersonList(personsFrom(listOfPersonStringsFrom(getRequiredLines(stringArrayFrom(gedcomFileMap(tree))))), firstName, surname)
            val sinkPersons: Future[List[GedcomPerson]] = sourcePersons.runWith(Sink.head)
            complete(sinkPersons)
          }
        },
      )
    }
  }

  val repo = new PersonRepository

  def convertGedcomToPerson(gedcomPerson: GedcomPerson):Person = {
    Person(gedcomPerson.firstName.getOrElse(""), gedcomPerson.surname.getOrElse(""), Timestamp.valueOf(LocalDateTime.now()), gedcomPerson.place.getOrElse(""), gedcomPerson.place.getOrElse(""), gedcomPerson.place.getOrElse(""), None, gedcomPerson.id.getOrElse("1").toLong, gedcomPerson.parentRelation.getOrElse("1").toLong, 1L, gedcomPerson.childRelation.getOrElse(List()).mkString(","), gedcomPerson.parentRelation.getOrElse(""), gedcomPerson.sex.getOrElse("M"))
  }

  def savePerson(p: Person): Person = {
    val insAct = repo.insert(p)
    p
  }

//  val originalPersons: Source[List[GedcomPerson], Future[IOResult]] = filteredPersonList(personsFrom(listOfPersonStringsFrom(getRequiredLines(stringArrayFrom(gedcomFileMap("london1"))))), "*", "*")
//  val doneOriginal = originalPersons.via(Flow[List[GedcomPerson]].map(p => p.map(c => convertGedcomToPerson(c)))).toMat(Sink.head)(Keep.right).run()
//  val dummyPerson = Person("", "", Timestamp.valueOf(LocalDateTime.now()),"", "","", None, 0, 0, 0, "", "", "M")
//  doneOriginal.onComplete {
//    case Success(persons) => {
//
//      def addParentIds(p: Person) = {
//        val newMotherId: Long = persons.find(per => per.childRelations.split(",").exists(p1 => p1.equals(p.parentRelation) && per.sex.equals("F") )).headOption.getOrElse(dummyPerson).personId
//        val newFatherId: Long = persons.find(per => per.childRelations.split(",").exists(p1 => p1.equals(p.parentRelation) && per.sex.equals("M") )).headOption.getOrElse(dummyPerson).personId
//        val newP = p.copy(motherId = newMotherId, fatherId = newFatherId)
//        newP
//      }
//
//      val sourcePersons: Source[List[GedcomPerson], Future[IOResult]] = filteredPersonList(personsFrom(listOfPersonStringsFrom(getRequiredLines(stringArrayFrom(gedcomFileMap("london1"))))), "*", "*")
//      val done = sourcePersons.via(Flow[List[GedcomPerson]].map(p => p.map(c => convertGedcomToPerson(c)).map(ps => addParentIds(ps)).map(per => savePerson(per)))).runForeach(person => println(person))
//      done.onComplete(_ => system.terminate())
//    }
//  }


  val person = cors() {
    path("persons" ) {
      concat(
        get {
          parameters('firstName ? "*", 'surname ? "*") { (firstName, surname) =>
            //     import martinetherton.PersonRepository
            //     val messagesResults = result(messagesFuture, 2.seconds)
            //          val sql = messages.result.statements.mkString

            val result = repo.getPersons(firstName, surname)
//            val filteredPersons = persons.map(listPersons => listPersons
//              .filter(person => (person.firstName.toLowerCase.contains(firstName.toLowerCase) || firstName.equals("*")) &&
//                (person.surname.toLowerCase.contains(surname.toLowerCase) || surname.equals("*"))))
//
//            val sinkPersons = filteredPersons.runWith(Sink.seq)

            complete(result)
          }
        },
        post {
          entity(as[Person]) { person =>
            val insAct = repo.insert(person)
            onComplete(insAct) { done =>
              complete(s"new person added with id: ${insAct}")
            }
          }
        },
      )
    }
  }

  val bindingFuture = Http().bindAndHandle(gedcom ~ person, "0.0.0.0", 8080)

  println(s"Server online at http://localhost:8080/\nPress RETURN to stop...")
  StdIn.readLine() // let it run until user presses return
  bindingFuture
    .flatMap(_.unbind()) // trigger unbinding from the port
    .onComplete(_ => system.terminate()) // and shutdown when done

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

  def personsFrom(personStringArrays: Source[List[List[String]], Future[IOResult]]): Source[List[GedcomPerson], Future[IOResult]] = {
    personStringArrays.map(a => a.map( arr1 => {
      var arr = arr1.reverse
      val name = arr.find(s => s.startsWith("1 NAME")).getOrElse("1 NAME ").toString.substring(7).split("/")
      val firstName = name(0)
      if (!firstName.equals("Ancestry.com")) {
        val surname = if (name.length > 1) name(1).replace("/", "") else ""
        val indexBirth = arr.indexOf("1 BIRT ")
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
        val parentRelationTemp = arr.find(s => s.startsWith("1 FAMC")).getOrElse("1 FAMC ").toString.substring(7).replace("@F", "").replace("@", "")
        val parentRelation = if (parentRelationTemp.equals("")) "0" else parentRelationTemp
        val idTemp = arr.find(s => s.startsWith("0 @P")).getOrElse("0 @P").toString.replace("0 @P", "").replace("@ INDI ", "")
        val id = if (idTemp.equals("")) "0" else idTemp

        GedcomPerson(Some(id), Some(firstName), Some(surname), Some(dateOfBirth), Some(placeOfBirth), Some(dateOfDeath), Some(placeOfDeath), Some(sex), Some(childRelations), Some(parentRelation))
      } else {
        GedcomPerson(Some("UNKNOWN"), Some("UNKNOWN"), Some("UNKNOWN"), Some("UNKNOWN"), Some("UNKNOWN"), Some("UNKNOWN"), Some("UNKNOWN"), Some("UNKNOWN"), Some(List("UNKNOWN")), Some("UNKNOWN"))
      }

    }))

  }

  def filteredPersonList(persons: Source[List[GedcomPerson], Future[IOResult]], firstName: String, surname: String): Source[List[GedcomPerson], Future[IOResult]] = {
    persons.map(listPersons => listPersons
      .filter(person => (person.firstName.getOrElse("").toLowerCase.contains(firstName.toLowerCase) || firstName.equals("*")) &&
        (person.surname.getOrElse("").toLowerCase.contains(surname.toLowerCase) || surname.equals("*"))))
  }



}