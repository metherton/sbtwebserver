package martinetherton.domain

import java.sql.Timestamp

case class LoserDB(id: Option[Long] = None, ticker: String, changes: Double, price: String, changesPercentage: String, companyName: String, insertTime: Timestamp) {


}
