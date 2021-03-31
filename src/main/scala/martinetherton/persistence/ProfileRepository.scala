package martinetherton.persistence

import java.sql.Timestamp

import martinetherton.domain.{LoserDB, ProfileDB}

import scala.concurrent.Future

class ProfileRepository {

  import slick.jdbc.MySQLProfile.api._

  class ProfileTable(tag: Tag) extends Table[ProfileDB](tag, "profile") {
    def id = column[Long]("id", O.PrimaryKey, O.AutoInc)
    def symbol = column[String]("symbol")
    def price = column[Double]("price")
    def beta = column[Double]("beta")
    def volAvg = column[Int]("volAvg")
    def mktCap = column[Int]("mktCap")
    def lastDiv = column[Double]("lastDiv")
    def range = column[String]("range")
    def changes = column[Double]("changes")
    def companyName = column[String]("companyName")
    def currency = column[String]("currency")
    def cik = column[String]("cik")
    def isin = column[String]("isin")
    def cusip = column[String]("cusip")
    def exchange = column[String]("exchange")
    def exchangeShortName = column[String]("exchangeShortName")
    def industry = column[String]("industry")
    def website = column[String]("website")
    def description = column[String]("description")
    def ceo = column[String]("ceo")
    def sector = column[String]("sector")
    def country = column[String]("country")
    def fullTimeEmployees = column[String]("fullTimeEmployees")
    def phone = column[String]("phone")
    def address = column[String]("address")
    def city = column[String]("city")
    def state = column[String]("state")
    def zip = column[String]("zip")
    def dcfDiff = column[String]("dcfDiff")
    def dcf = column[Double]("dcf")
    def image = column[String]("image")
    def ipoDate = column[String]("ipoDate")
    def defaultImage = column[Boolean]("defaultImage")
    def isEtf = column[Boolean]("isEtf")
    def isActivelyTrading = column[Boolean]("isActivelyTrading")
    def insertTime = column[Timestamp]("insertTime")
    def * = (id.?, symbol, price, beta, volAvg, mktCap, lastDiv, range, changes, companyName,
      currency, cik, isin, cusip, exchange, exchangeShortName, industry, website, description, ceo,
      sector, country, fullTimeEmployees, phone, address, city, state, zip, dcfDiff, dcf, image, ipoDate,
      defaultImage, isEtf, isActivelyTrading, insertTime).mapTo[ProfileDB]
  }

  val profiles = TableQuery[ProfileTable]

  val db = Database.forConfig("ons")

  def exec[T](action: DBIO[T]): Future[T] =
    db.run(action)

  def getProfiles() = db.run(profiles.result)

  def insert(profilesToInsert: List[ProfileDB]) = {
    val ins = profiles ++= profilesToInsert
    db.run(ins)
  }

}
