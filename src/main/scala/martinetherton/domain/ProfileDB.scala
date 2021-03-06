package martinetherton.domain

import java.sql.Timestamp

case class ProfileDB(id: Option[Long] = None, symbol: Option[String], price: Option[Double], beta: Option[Double], volAvg: Option[Long], mktCap: Option[Long], lastDiv: Option[Double], range: Option[String], changes: Option[Double],
                     companyName: Option[String], currency: Option[String], cik: Option[String], isin: Option[String], cusip: Option[String], exchange: Option[String], exchangeShortName: Option[String], industry: Option[String],
                     website: Option[String], description: Option[String], ceo: Option[String], sector: Option[String], country: Option[String], fullTimeEmployees: Option[String], phone: Option[String], address: Option[String],
                     city: Option[String], state: Option[String], zip: Option[String], dcfDiff: Option[Double], dcf: Option[Double], image: Option[String], ipoDate: Option[String], defaultImage: Option[Boolean], isEtf: Option[Boolean], isActivelyTrading: Option[Boolean],
                     insertTime: Timestamp) {

}
