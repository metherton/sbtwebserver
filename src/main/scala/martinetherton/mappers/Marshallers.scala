package martinetherton.mappers

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import martinetherton.domain._
import spray.json.DefaultJsonProtocol

trait Marshallers extends DefaultJsonProtocol  with SprayJsonSupport {

  implicit val stockFormat = jsonFormat2(Stock)
  implicit val executiveFormat = jsonFormat8(Executive)
  implicit val symbolNameFormat = jsonFormat2(SymbolName)
  implicit val currencyExchangeRateFormat = jsonFormat8(CurrencyExchangeRate)
  implicit val sectorChangeFormat = jsonFormat2(SectorChange)
  implicit val sectorPerformanceFormat = jsonFormat1(SectorPerformance)
  implicit val loserFormat = jsonFormat5(Loser)

//  implicit val itemFormat = jsonFormat2(Item)
//  implicit val orderFormat = jsonFormat1(Order)
//  implicit val itemVoFormat = jsonFormat2(ItemVo)

}
