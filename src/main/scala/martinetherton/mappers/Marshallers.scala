package martinetherton.mappers

import java.sql.Timestamp

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import martinetherton.domain._
import spray.json.{DefaultJsonProtocol, DeserializationException, JsNumber, JsValue, JsonFormat}

trait Marshallers extends DefaultJsonProtocol  with SprayJsonSupport {

  implicit object TimestampFormat extends JsonFormat[Timestamp] {
    def write(obj: Timestamp) = JsNumber(obj.getTime)

    def read(json: JsValue) = json match {
      case JsNumber(time) => new Timestamp(time.toLong)

      case _ => throw new DeserializationException("Date expected")
    }
  }

  implicit val stockFormat = jsonFormat2(Stock)
  implicit val executiveFormat = jsonFormat8(Executive)
  implicit val symbolNameFormat = jsonFormat2(SymbolName)
  implicit val currencyExchangeRateFormat = jsonFormat8(CurrencyExchangeRate)
  implicit val sectorChangeFormat = jsonFormat2(SectorChange)
  implicit val sectorPerformanceFormat = jsonFormat1(SectorPerformance)
  implicit val loserFormat = jsonFormat5(Loser)
  implicit val loserFormatDB = jsonFormat7(LoserDB)
  implicit val userFormat = jsonFormat3(User)

//  implicit val itemFormat = jsonFormat2(Item)
//  implicit val orderFormat = jsonFormat1(Order)
//  implicit val itemVoFormat = jsonFormat2(ItemVo)

}
