package martinetherton.domain

import java.sql.Timestamp

case class StockDB(id: Option[Long] = None, symbol: String, price: Double, insertTime: Timestamp) {


}
