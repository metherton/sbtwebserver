package martinetherton.persistence

import java.sql.Timestamp

import martinetherton.domain.{LoserDB, StockDB}

import scala.concurrent.Future

class StockRepository {

  import slick.jdbc.MySQLProfile.api._

  class StockTable(tag: Tag) extends Table[StockDB](tag, "stock") {
    def id = column[Long]("id", O.PrimaryKey, O.AutoInc)
    def symbol = column[String]("symbol")
    def price = column[Double]("price")
    def insertTime = column[Timestamp]("insertTime")
    def * = (id.?, symbol, price, insertTime).mapTo[StockDB]
  }

  val stocks = TableQuery[StockTable]
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

  def getAllStocks() = {
    db.run(stocks.result)
  }

  def insert(stocksToInsert: List[StockDB]) = {
    val ins = stocks ++= stocksToInsert
    db.run(ins)
  }

}
