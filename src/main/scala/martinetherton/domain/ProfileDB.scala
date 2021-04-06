package martinetherton.domain

import java.sql.Timestamp

case class ProfileDB(id: Option[Long] = None, symbol: String, price: Double, beta: Double, volAvg: Long, mktCap: Long, lastDiv: Double, range: String, changes: Double,
                     companyName: String, currency: String, cik: Option[String], isin: Option[String], cusip: Option[String], exchange: String, exchangeShortName: String, industry: String,
                     website: String, description: Option[String], ceo: String, sector: String, country: Option[String], fullTimeEmployees: Option[String], phone: Option[String], address: Option[String],
                     city: Option[String], state: Option[String], zip: Option[String], dcfDiff: Option[Double], dcf: Double, image: String, ipoDate: Option[String], defaultImage: Boolean, isEtf: Boolean, isActivelyTrading: Boolean,
                     insertTime: Timestamp) {

}
