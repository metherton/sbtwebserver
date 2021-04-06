package martinetherton.mappers


import java.sql.Timestamp

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import martinetherton.domain._
import spray.json
import spray.json._
import spray.json.JsNull

trait Marshallers extends DefaultJsonProtocol  with SprayJsonSupport with NullOptions {

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
        "cik" -> p.cik.map(value => JsString(value)).getOrElse(JsNull),
        "isin" -> p.isin.map(value => JsString(value)).getOrElse(JsNull),
        "cusip" -> p.cusip.map(value => JsString(value)).getOrElse(JsNull),
        "exchange" -> JsString(p.exchange),
        "exchangeShortName" -> JsString(p.exchangeShortName),
        "industry" -> JsString(p.industry),
        "website" -> JsString(p.website),
        "description" -> p.description.map(value => JsString(value)).getOrElse(JsNull),
        "ceo" -> JsString(p.ceo),
        "sector" -> JsString(p.sector),
        "country" -> p.country.map(value => JsString(value)).getOrElse(JsNull),
        "fullTimeEmployees" -> p.fullTimeEmployees.map(value => JsString(value)).getOrElse(JsNull),
        "phone" -> p.phone.map(value => JsString(value)).getOrElse(JsNull),
        "address" -> p.address.map(value => JsString(value)).getOrElse(JsNull),
        "city" -> p.city.map(value => JsString(value)).getOrElse(JsNull),
        "state" -> p.state.map(value => JsString(value)).getOrElse(JsNull),
        "zip" -> p.zip.map(value => JsString(value)).getOrElse(JsNull),
        "dcfDiff" -> p.dcfDiff.map(value => JsNumber(value)).getOrElse(JsNull),
        "dcf" -> JsNumber(p.dcf),
        "image" -> JsString(p.image),
        "ipoDate" -> p.ipoDate.map(value => JsString(value)).getOrElse(JsNull),
        "defaultImage" -> JsBoolean(p.defaultImage),
        "isEtf" -> JsBoolean(p.isEtf),
        "isActivelyTrading" -> JsBoolean(p.isActivelyTrading)
      )

    def read(value: JsValue) = {
      import spray.json._
      val values: Seq[JsValue] = value.asJsObject.getFields("symbol", "price", "beta", "volAvg", "mktCap", "lastDiv", "range", "changes", "companyName", "currency", "cik", "isin", "cusip", "exchange", "exchangeShortName", "industry", "website", "description", "ceo", "sector", "country", "fullTimeEmployees", "phone", "address", "city", "state", "zip", "dcfDiff", "dcf", "image", "ipoDate", "defaultImage", "isEtf", "isActivelyTrading")

      var i = 0
      // zip
      val newValues = values.map(value => {
        if (values(i) == JsNull) {
          i += 1
          if (Set(11, 12, 13, 18, 21, 22, 23, 24, 25, 26, 27, 31).contains(i)) JsString("")
          else if (Set(28).contains(i)) JsNumber(0)
        } else {
          i += 1
          value
        }
      })
      newValues match {
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
          JsNumber(dcfDiff),
          JsNumber(dcf),
          JsString(image),
          JsString(ipoDate),
          JsBoolean(defaultImage),
          JsBoolean(isEtf),
          JsBoolean(isActivelyTrading))))))))))))))) =>
          {
            //val zipO = if (zip == null) "" else zip
            new Profile(symbol, price.toDouble, beta.toDouble, volAvg.toLong, mktCap.toLong, lastDiv.toDouble, range, changes.toDouble,
              companyName, currency, Some(cik), Some(isin), Some(cusip), exchange, exchangeShortName, industry,
              website, Some(description), ceo, sector, Some(country), Some(fullTimeEmployees), Some(phone), Some(address),
              Some(city), Some(state), Some(zip), Some(dcfDiff.toDouble), dcf.toDouble, image, Some(ipoDate), defaultImage, isEtf, isActivelyTrading)
          }
        case ex => throw new DeserializationException(ex.toString())
      }
    }
  }

}
