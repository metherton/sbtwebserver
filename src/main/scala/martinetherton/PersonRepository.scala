package martinetherton

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import slick.jdbc.H2Profile.api._
import martinetherton.Person

class PersonRepository {

  class PersonTable(tag: Tag) extends Table[Person](tag, "person") {
    def id = column[Long]("id", O.PrimaryKey, O.AutoInc)
    def firstName = column[String]("firstName")
    def surname = column[String]("surname")
    def * = (firstName, surname, id.?).mapTo[Person]
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
    Person("Martin", "Etherton", None),
    Person("Sydney", "Etherton", None),
    Person("Sydney", "Etherton", None),
    Person("Samuel", "Etherton", None)
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
