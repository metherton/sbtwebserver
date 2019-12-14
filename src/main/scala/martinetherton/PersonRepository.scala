package martinetherton

import java.sql.Timestamp

import akka.actor.ActorSystem
import org.joda.time.DateTime
import akka.stream.ActorMaterializer
import slick.jdbc.H2Profile.api._
import martinetherton.Person

class PersonRepository {

  class PersonTable(tag: Tag) extends Table[Person](tag, "person") {
    def id = column[Long]("id", O.PrimaryKey, O.AutoInc)
    def firstName = column[String]("firstName")
    def surname = column[String]("surname")
    def dateOfBirth = column[Timestamp]("dateOfBirth")
    def * = (firstName, surname, id.?, dateOfBirth).mapTo[Person]
  }

  val persons = TableQuery[PersonTable]
  val martin = persons.filter(_.firstName === "Martin")

  val db = Database.forConfig("ons")

  persons.schema.createStatements.mkString
  val action: DBIO[Unit] = persons.schema.create

  import scala.concurrent.Future
  val future: Future[Unit] = db.run(action)
  import scala.concurrent.Await
  import scala.concurrent.duration._
  val result = Await.result(future, 2.seconds)

  def freshTestData = Seq(
    Person("Martin", "Etherton", None, new Timestamp(DateTime.now.getMillis)),
    Person("Sydney", "Etherton", None, new Timestamp(DateTime.now.getMillis)),
    Person("Sydney", "Etherton", None, new Timestamp(DateTime.now.getMillis)),
    Person("Samuel", "Etherton", None, new Timestamp(DateTime.now.getMillis))
  )

  val insert: DBIO[Option[Int]] = persons ++= freshTestData
  val insertAction: Future[Option[Int]] = db.run(insert)

  val rowCount = Await.result(insertAction, 2.seconds)

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
