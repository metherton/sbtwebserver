package martinetherton.persistence

import java.sql.Timestamp

import martinetherton.domain.{Loser, LoserDB, Person}

import scala.concurrent.Future

class LoserRepository {

  import slick.jdbc.MySQLProfile.api._

  class LoserTable(tag: Tag) extends Table[LoserDB](tag, "loser") {
    def id = column[Long]("id", O.PrimaryKey, O.AutoInc)
    def ticker = column[String]("ticker")
    def changes = column[Double]("changes")
    def price = column[String]("price")
    def changesPercentage = column[String]("changesPercentage")
    def companyName = column[String]("companyName")
    def insertTime = column[Timestamp]("insertTime")
    def * = (id.?, ticker, changes, price, changesPercentage, companyName, insertTime).mapTo[LoserDB]
  }

  val losers = TableQuery[LoserTable]
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

  def getAllLosers() = db.run(losers.result)

  def insert(losersToInsert: List[LoserDB]) = {
    val ins = losers ++= losersToInsert
    db.run(ins)
  }

}
