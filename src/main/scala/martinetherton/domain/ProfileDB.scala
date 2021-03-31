package martinetherton.domain

import java.sql.Timestamp

case class ProfileDB(id: Option[Long] = None, symbol: String, price: Double, beta: Double, volAvg: Int, mktCap: Int, lastDiv: Double, range: String, changes: Double,
                     companyName: String, currency: String, cik: String, isin: String, cusip: String, exchange: String, exchangeShortName: String, industry: String,
                     website: String, description: String, ceo: String, sector: String, country: String, fullTimeEmployees: String, phone: String, address: String,
                     city: String, state: String, zip: String, dcfDiff: String, dcf: Double, image: String, ipoDate: String, defaultImage: Boolean, isEtf: Boolean, isActivelyTrading: Boolean,
                     insertTime: Timestamp) {

}
