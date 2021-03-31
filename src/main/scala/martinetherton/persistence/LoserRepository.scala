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
    def * = (id.?, ticker, changes, price, changesPercentage,
      companyName, insertTime).mapTo[LoserDB]
  }

  val losers = TableQuery[LoserTable]

  val db = Database.forConfig("ons")

  def exec[T](action: DBIO[T]): Future[T] =
    db.run(action)

  def getAllLosers() = db.run(losers.result)

  def insert(losersToInsert: List[LoserDB]) = {
    val ins = losers ++= losersToInsert
    db.run(ins)
  }

}
