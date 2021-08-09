package martinetherton.actors

import java.nio.file.{Path, Paths}
import java.sql.{Date, Timestamp}
import java.time.LocalDateTime

import akka.actor.{Actor, ActorLogging, ActorSystem}
import akka.stream.{ActorMaterializer, IOResult}
import akka.stream.scaladsl.{FileIO, Flow, Framing, Keep, Sink, Source}
import akka.util.ByteString
import martinetherton.actors.TreeImporter.ImportTree
import martinetherton.domain.{GedcomPerson, Person}
import martinetherton.mappers.Marshallers
import martinetherton.persistence.PersonRepository
import spray.json.{DeserializationException, JsNumber, JsValue, JsonFormat}

import scala.concurrent.Future
import scala.util.{Failure, Success}


object TreeImporter {
  case object ImportTree
}

class TreeImporter extends Actor with ActorLogging with Marshallers {
//class TreeImporter {

  implicit val system = ActorSystem("TreeImporter")
  implicit val materializer = ActorMaterializer()
  import system.dispatcher

  val repo = new PersonRepository

  val gedcomFileMap =
    Map("london1" -> "etherton-london-1.ged",
      "sussex1" -> "etherton-sussex-1.ged",
      "london2" -> "etherton-london-2.ged",
      "sussex2" -> "etherton-sussex-2.ged",
      "usa1" -> "etherton-usa-1.ged")

  def filteredPersonList(persons: Source[List[GedcomPerson], Future[IOResult]], firstName: String, surname: String): Source[List[GedcomPerson], Future[IOResult]] = {
    persons.map(listPersons => listPersons
      .filter(person => (person.firstName.getOrElse("").toLowerCase.contains(firstName.toLowerCase) || firstName.equals("*")) &&
        (person.surname.getOrElse("").toLowerCase.contains(surname.toLowerCase) || surname.equals("*"))))
  }

  def isMonth(name: String): Boolean = {
    if (name.toLowerCase().equals("july") || name.toLowerCase().equals("dec.") || name.toLowerCase().equals("june")) true else false
  }

  def convertDate(dateString: String): String = {
    val monMap: Map[String, Int] = Map("jan" -> 1, "feb" -> 2, "mar" -> 3, "apr" -> 4, "may" -> 5, "jun" -> 6, "jul" -> 7,
    "aug" -> 8, "sep" -> 9, "oct" -> 10, "nov" -> 11, "dec" -> 12, "january" -> 1, "february" -> 2, "march" -> 3, "april" -> 4, "may" -> 5, "june" -> 6, "july" -> 7,
      "august" -> 8, "september" -> 9, "october" -> 10, "november" -> 11, "december" -> 12
    )
    var day: Int = 1
    var month: Int = 1
    var y: Int = 1970
    var newDateString = dateString.replace("Abt.", "").trim
    val dateParts: List[String] = newDateString.split(" ").toList
    val datePartsSlash: List[String] = newDateString.split("/").toList
    if (datePartsSlash.length > 2) {
      month = datePartsSlash(0).toInt
      day = datePartsSlash(1).toInt
      y = datePartsSlash(2).toInt
    } else if (dateParts.length > 0) {
      println(newDateString)
      val newYear: List[String] = dateParts.filter(part => part.trim.length == 4).filter((yr) => !isMonth(yr))
      if (newYear.length > 0) {
        y = newYear.head.toInt
      }
      val mon = dateParts.filter(part => part.trim.length == 3 && part.toLowerCase != "abt" && !part.contains(","))
      if (mon.length == 1) {
        month = monMap(mon.head.toLowerCase)
      }
      val days = dateParts.filter(part => part.trim.length == 1 || part.trim.length == 2)
      if (days.length == 1) {
        day = days.head.replace(",","").toInt
      }
    }
    println(s"${y.toString}-${month.toString}-${day.toString}")
    s"${y.toString}-${month.toString}-${day.toString}"
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

        GedcomPerson(Some(id), Some(firstName), Some(surname), Some(convertDate(dateOfBirth)), Some(placeOfBirth), Some(dateOfDeath), Some(placeOfDeath), Some(sex), Some(childRelations), Some(parentRelation))
      } else {
        GedcomPerson(Some("UNKNOWN"), Some("UNKNOWN"), Some("UNKNOWN"), Some("UNKNOWN"), Some("UNKNOWN"), Some("UNKNOWN"), Some("UNKNOWN"), Some("UNKNOWN"), Some(List("UNKNOWN")), Some("UNKNOWN"))
      }

    }))

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

  def convertLocalDateTime(dob: String): LocalDateTime = {
    val dateParts: List[String] = dob.split("-").toList
    LocalDateTime.of(dateParts(0).toInt, dateParts(1).toInt, dateParts(2).toInt, 23, 59)
  }

  def convertGedcomToPerson(gedcomPerson: GedcomPerson):Person = {
//    Person(gedcomPerson.firstName.getOrElse("").trim(), gedcomPerson.surname.getOrElse(""), Timestamp.valueOf(LocalDateTime.now()), gedcomPerson.place.getOrElse(""), gedcomPerson.place.getOrElse(""), gedcomPerson.place.getOrElse(""), None, gedcomPerson.id.getOrElse("1").toLong, gedcomPerson.parentRelation.getOrElse("1").toLong, 1L, gedcomPerson.childRelation.getOrElse(List()).mkString(","), gedcomPerson.parentRelation.getOrElse(""), gedcomPerson.sex.getOrElse("M"))
    Person(gedcomPerson.firstName.getOrElse("").trim(), gedcomPerson.surname.getOrElse(""), Timestamp.valueOf(convertLocalDateTime(gedcomPerson.dateOfBirth.getOrElse("1970-1-1"))), gedcomPerson.place.getOrElse(""), gedcomPerson.place.getOrElse(""), gedcomPerson.place.getOrElse(""), None, gedcomPerson.id.getOrElse("1").toLong, gedcomPerson.parentRelation.getOrElse("1").toLong, 1L, gedcomPerson.childRelation.getOrElse(List()).mkString(","), gedcomPerson.parentRelation.getOrElse(""), gedcomPerson.sex.getOrElse("M"))

  }

  def getRequiredLines(stringSource: Source[String, Future[IOResult]]): Source[String, Future[IOResult]] = {
    stringSource.filter(row => List("2 PLAC", "2 DATE", "1 BIRT", "1 SEX", "0 @P", "1 NAME", "1 DEAT", "1 FAMC", "1 FAMS").exists(prefix => row.startsWith(prefix)))
  }

  def stringArrayFrom(gedcomFile: String): Source[String, Future[IOResult]] = {
    val file: Path = Paths.get(ClassLoader.getSystemResource(gedcomFile).toURI)
    val source: Source[ByteString, Future[IOResult]] = FileIO.fromPath(file)
    val byteStringArray: Source[ByteString, Future[IOResult]] = source.via(Framing.delimiter(
      ByteString("\r\n"), maximumFrameLength = 500, allowTruncation = true))
    val stringArray = byteStringArray.map(_.utf8String)
    stringArray
  }



  override def receive: Receive = {
    case ImportTree => {
      log.info("importing tree")
      val originalPersons: Source[List[GedcomPerson], Future[IOResult]] = filteredPersonList(personsFrom(listOfPersonStringsFrom(getRequiredLines(stringArrayFrom(gedcomFileMap("usa1"))))), "*", "*")
      val doneOriginal = originalPersons.via(Flow[List[GedcomPerson]].map(p => p.map(c => convertGedcomToPerson(c)))).toMat(Sink.head)(Keep.right).run()
      val dummyPerson = Person("", "", Timestamp.valueOf(LocalDateTime.now()),"", "","", None, 0, 0, 0, "", "", "M")
      doneOriginal.onComplete {
        case Success(persons) => {

          def addParentIds(p: Person) = {
            val newMotherId: Long = persons.find(per => per.childRelations.split(",").exists(p1 => p1.equals(p.parentRelation) && per.sex.equals("F") )).headOption.getOrElse(dummyPerson).personId
            val newFatherId: Long = persons.find(per => per.childRelations.split(",").exists(p1 => p1.equals(p.parentRelation) && per.sex.equals("M") )).headOption.getOrElse(dummyPerson).personId
            val newP = p.copy(motherId = newMotherId, fatherId = newFatherId)
            newP
          }

          val sourcePersons: Source[List[GedcomPerson], Future[IOResult]] = filteredPersonList(personsFrom(listOfPersonStringsFrom(getRequiredLines(stringArrayFrom(gedcomFileMap("usa1"))))), "*", "*")
          val done = sourcePersons.via(Flow[List[GedcomPerson]].map(p => p.map(c => convertGedcomToPerson(c)).map(ps => addParentIds(ps)).map(per => savePerson(per)))).runForeach(person => println(person))
          done.onComplete(_ => system.terminate())
        }
        case Failure(exception) => println(exception)
      }

      def savePerson(p: Person): Person = {
        val insAct = repo.insert(p)
        p
      }











    }
  }
}
