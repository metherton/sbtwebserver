package martinetherton.mappers

import java.sql.Timestamp

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import martinetherton.domain._
import spray.json._

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


  implicit object ProfileJsonFormat extends RootJsonFormat[Profile] {
    def write(p: Profile) =
      JsObject(
        "symbol" -> JsString(p.symbol),
        "price" -> JsNumber(p.price),
        "beta" -> JsNumber(p.beta),
        "volAvg" -> JsNumber(p.volAvg),
        "mktCap" -> JsNumber(p.mktCap),
        "lastDiv" -> JsNumber(p.lastDiv),
        "range" -> JsString(p.range),
        "changes" -> JsNumber(p.changes),
        "companyName" -> JsString(p.companyName),
        "currency" -> JsString(p.currency),
        "cik" -> JsString(p.cik),
        "isin" -> JsString(p.isin),
        "cusip" -> JsString(p.cusip),
        "exchange" -> JsString(p.exchange),
        "exchangeShortName" -> JsString(p.exchangeShortName),
        "industry" -> JsString(p.industry),
        "website" -> JsString(p.website),
        "description" -> JsString(p.description),
        "ceo" -> JsString(p.ceo),
        "sector" -> JsString(p.sector),
        "country" -> JsString(p.country),
        "fullTimeEmployees" -> JsString(p.fullTimeEmployees),
        "phone" -> JsString(p.phone),
        "address" -> JsString(p.address),
        "city" -> JsString(p.city),
        "state" -> JsString(p.state),
        "zip" -> JsString(p.zip),
        "dcfDiff" -> JsString(p.dcfDiff),
        "dcf" -> JsNumber(p.dcf),
        "image" -> JsString(p.image),
        "ipoDate" -> JsString(p.ipoDate),
        "defaultImage" -> JsBoolean(p.defaultImage),
        "isEtf" -> JsBoolean(p.isEtf),
        "isActivelyTrading" -> JsBoolean(p.isActivelyTrading)
      )

    def read(value: JsValue) = {
      value.asJsObject.getFields("symbol", "price", "beta", "volAvg", "mktCap", "lastDiv", "range", "changes", "companyName", "currency", "cik", "isin", "cusip", "exchange", "exchangeShortName", "industry", "website", "description", "ceo", "sector", "country", "fullTimeEmployees", "phone", "address", "city", "state", "zip", "dcfDiff", "dcf", "image", "ipoDate", "defaultImage", "isEtf", "isActivelyTrading") match {
        case JsString(symbol) +: (JsNumber(price) +: (JsNumber(beta) +: (JsNumber(volAvg) +: (JsNumber(mktCap) +: (JsNumber(lastDiv) +:
          (JsString(range) +: (JsNumber(changes) +: (JsString(companyName) +: (JsString(currency) +: (JsString(cik) +: (JsString(isin) +:
            (JsString(cusip) +: (JsString(exchange) +: Seq(JsString(exchangeShortName),
          JsString(industry),
          JsString(website),
          JsString(description),
          JsString(ceo),
          JsString(sector),
          JsString(country),
          JsString(fullTimeEmployees),
          JsString(phone),
          JsString(address),
          JsString(city),
          JsString(state),
          JsString(zip),
          JsString(dcfDiff),
          JsNumber(dcf),
          JsString(image),
          JsString(ipoDate),
          JsBoolean(defaultImage),
          JsBoolean(isEtf),
          JsBoolean(isActivelyTrading))))))))))))))) =>
          new Profile(symbol, price.toDouble, beta.toDouble, volAvg.toInt, mktCap.toInt, lastDiv.toDouble, range, changes.toDouble,
            companyName, currency, cik, isin, cusip, exchange, exchangeShortName, industry,
            website, description, ceo, sector, country, fullTimeEmployees, phone, address,
            city, state, zip, dcfDiff, dcf.toDouble, image, ipoDate, defaultImage, isEtf, isActivelyTrading)
        case _ => throw new DeserializationException("error")
      }
    }
  }

}
