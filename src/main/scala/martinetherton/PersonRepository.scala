package martinetherton

import java.sql.Timestamp
import scala.concurrent.Future

class PersonRepository {

  import slick.jdbc.MySQLProfile.api._

  class PersonTable(tag: Tag) extends Table[Person](tag, "person") {
    def id = column[Long]("id", O.PrimaryKey, O.AutoInc)
    def firstName = column[String]("firstName")
    def surname = column[String]("surname")
    def dateOfBirth = column[Timestamp]("dateOfBirth")
    def address = column[String]("address")
    def city = column[String]("city")
    def country = column[String]("country")
    def personId = column[Long]("personId")
    def fatherId = column[Long]("fatherId")
    def motherId = column[Long]("motherId")
    def childRelations = column[String]("childRelations")
    def parentRelation = column[String]("parentRelation")
    def sex = column[String]("sex")

    def * = (firstName, surname, dateOfBirth, address, city, country, id.?, personId, fatherId, motherId, childRelations, parentRelation, sex).mapTo[Person]
  }

  val persons = TableQuery[PersonTable]
  //  val martin = persons.filter(_.firstName === "Martin")

  val db = Database.forConfig("ons")

  //  persons.schema.createStatements.mkString
  //  val action: DBIO[Unit] = persons.schema.create

  //  import scala.concurrent.Future
  //  val future: Future[Unit] = db.run(action)
  //  import scala.concurrent.Await
  //  import scala.concurrent.duration._
  //  val result = Await.result(future, 2.seconds)

  //  def freshTestData = Seq(
  //    Person("Martin", "Etherton", new Timestamp(DateTime.now.getMillis), "Greenwood Drive", "Sheffield", "England", None),
  //    Person("Sydney", "Etherton", new Timestamp(DateTime.now.getMillis), "Addy Street", "Sheffield", "England", None),
  //    Person("Sydney", "Etherton", new Timestamp(DateTime.now.getMillis), "Rusholme Road", "Manchester", "England", None),
  //    Person("Samuel", "Etherton", new Timestamp(DateTime.now.getMillis), "City Road", "London", "England", None)
  //  )

  //  val insert: DBIO[Option[Int]] = persons ++= freshTestData
  //  val insertAction: Future[Option[Int]] = db.run(insert)

  //  val rowCount = Await.result(insertAction, 2.seconds)

  def exec[T](action: DBIO[T]): Future[T] =
    db.run(action)

  def getPersons() = {
    val personsAction: DBIO[Seq[Person]] = persons.result
    val personsFuture: Future[Seq[Person]] = db.run(personsAction)
    personsFuture
  }

  def insert(person: Person) = {
    val ins = persons returning persons.map(_.id) += person
    val insAct = exec(ins)
    insAct
  }

}
