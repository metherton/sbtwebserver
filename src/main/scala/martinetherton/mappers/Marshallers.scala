package martinetherton.mappers


import java.sql.Timestamp

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import martinetherton.domain._
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




  implicit val gedcomPersonFormat = jsonFormat10(GedcomPerson)

  implicit val personFormat = jsonFormat13(Person)




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
        "symbol" -> p.symbol.map(value => JsString(value)).getOrElse(JsNull),
        "price" -> p.price.map(value => JsNumber(value)).getOrElse(JsNull),
        "beta" -> p.beta.map(value => JsNumber(value)).getOrElse(JsNull),
        "volAvg" -> p.volAvg.map(value => JsNumber(value)).getOrElse(JsNull),
        "mktCap" -> p.mktCap.map(value => JsNumber(value)).getOrElse(JsNull),
        "lastDiv" -> p.lastDiv.map(value => JsNumber(value)).getOrElse(JsNull),
        "range" -> p.range.map(value => JsString(value)).getOrElse(JsNull),
        "changes" -> p.changes.map(value => JsNumber(value)).getOrElse(JsNull),
        "companyName" -> p.companyName.map(value => JsString(value)).getOrElse(JsNull),
        "currency" -> p.currency.map(value => JsString(value)).getOrElse(JsNull),
        "cik" -> p.cik.map(value => JsString(value)).getOrElse(JsNull),
        "isin" -> p.isin.map(value => JsString(value)).getOrElse(JsNull),
        "cusip" -> p.cusip.map(value => JsString(value)).getOrElse(JsNull),
        "exchange" -> p.exchange.map(value => JsString(value)).getOrElse(JsNull),
        "exchangeShortName" -> p.exchangeShortName.map(value => JsString(value)).getOrElse(JsNull),
        "industry" -> p.industry.map(value => JsString(value)).getOrElse(JsNull),
        "website" -> p.website.map(value => JsString(value)).getOrElse(JsNull),
        "description" -> p.description.map(value => JsString(value)).getOrElse(JsNull),
        "ceo" -> p.ceo.map(value => JsString(value)).getOrElse(JsNull),
        "sector" -> p.sector.map(value => JsString(value)).getOrElse(JsNull),
        "country" -> p.country.map(value => JsString(value)).getOrElse(JsNull),
        "fullTimeEmployees" -> p.fullTimeEmployees.map(value => JsString(value)).getOrElse(JsNull),
        "phone" -> p.phone.map(value => JsString(value)).getOrElse(JsNull),
        "address" -> p.address.map(value => JsString(value)).getOrElse(JsNull),
        "city" -> p.city.map(value => JsString(value)).getOrElse(JsNull),
        "state" -> p.state.map(value => JsString(value)).getOrElse(JsNull),
        "zip" -> p.zip.map(value => JsString(value)).getOrElse(JsNull),
        "dcfDiff" -> p.dcfDiff.map(value => JsNumber(value)).getOrElse(JsNull),
        "dcf" -> p.dcf.map(value => JsNumber(value)).getOrElse(JsNull),
        "image" -> p.image.map(value => JsString(value)).getOrElse(JsNull),
        "ipoDate" -> p.ipoDate.map(value => JsString(value)).getOrElse(JsNull),
        "defaultImage" -> p.defaultImage.map(value => JsBoolean(value)).getOrElse(JsNull),
        "isEtf" -> p.isEtf.map(value => JsBoolean(value)).getOrElse(JsNull),
        "isActivelyTrading" -> p.isActivelyTrading.map(value => JsBoolean(value)).getOrElse(JsNull),
      )

    def read(value: JsValue) = {
      import spray.json._
      val values: Seq[JsValue] = value.asJsObject.getFields("symbol", "price", "beta", "volAvg", "mktCap", "lastDiv", "range", "changes", "companyName", "currency", "cik", "isin", "cusip", "exchange", "exchangeShortName", "industry", "website", "description", "ceo", "sector", "country", "fullTimeEmployees", "phone", "address", "city", "state", "zip", "dcfDiff", "dcf", "image", "ipoDate", "defaultImage", "isEtf", "isActivelyTrading")

      var i = 0
      // zip
      val newValues = values.map(value => {
        if (values(i) == JsNull) {
          i += 1
          if (Set(1, 7, 8, 9, 10, 11, 12, 13, 14, 14, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 30, 31).contains(i)) JsString("")
          else if (Set(28, 29).contains(i)) JsNumber(0)
          else if (Set(32, 33, 34).contains(i)) JsBoolean(false)
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
            new Profile(Some(symbol), Some(price.toDouble), Some(beta.toDouble), Some(volAvg.toLong), Some(mktCap.toLong), Some(lastDiv.toDouble), Some(range), Some(changes.toDouble),
              Some(companyName), Some(currency), Some(cik), Some(isin), Some(cusip), Some(exchange), Some(exchangeShortName), Some(industry),
              Some(website), Some(description), Some(ceo), Some(sector), Some(country), Some(fullTimeEmployees), Some(phone), Some(address),
              Some(city), Some(state), Some(zip), Some(dcfDiff.toDouble), Some(dcf.toDouble), Some(image), Some(ipoDate), Some(defaultImage), Some(isEtf), Some(isActivelyTrading))
          }
        case ex => throw new DeserializationException(ex.toString())
      }
    }
  }

  implicit object ProfileDBJsonFormat extends RootJsonFormat[ProfileDB] {
    def write(p: ProfileDB) =
      JsObject(
        "id" -> JsNull,
        "symbol" -> p.symbol.map(value => JsString(value)).getOrElse(JsNull),
        "price" -> p.price.map(value => JsNumber(value)).getOrElse(JsNull),
        "beta" -> p.beta.map(value => JsNumber(value)).getOrElse(JsNull),
        "volAvg" -> p.volAvg.map(value => JsNumber(value)).getOrElse(JsNull),
        "mktCap" -> p.mktCap.map(value => JsNumber(value)).getOrElse(JsNull),
        "lastDiv" -> p.lastDiv.map(value => JsNumber(value)).getOrElse(JsNull),
        "range" -> p.range.map(value => JsString(value)).getOrElse(JsNull),
        "changes" -> p.changes.map(value => JsNumber(value)).getOrElse(JsNull),
        "companyName" -> p.companyName.map(value => JsString(value)).getOrElse(JsNull),
        "currency" -> p.currency.map(value => JsString(value)).getOrElse(JsNull),
        "cik" -> p.cik.map(value => JsString(value)).getOrElse(JsNull),
        "isin" -> p.isin.map(value => JsString(value)).getOrElse(JsNull),
        "cusip" -> p.cusip.map(value => JsString(value)).getOrElse(JsNull),
        "exchange" -> p.exchange.map(value => JsString(value)).getOrElse(JsNull),
        "exchangeShortName" -> p.exchangeShortName.map(value => JsString(value)).getOrElse(JsNull),
        "industry" -> p.industry.map(value => JsString(value)).getOrElse(JsNull),
        "website" -> p.website.map(value => JsString(value)).getOrElse(JsNull),
        "description" -> p.description.map(value => JsString(value)).getOrElse(JsNull),
        "ceo" -> p.ceo.map(value => JsString(value)).getOrElse(JsNull),
        "sector" -> p.sector.map(value => JsString(value)).getOrElse(JsNull),
        "country" -> p.country.map(value => JsString(value)).getOrElse(JsNull),
        "fullTimeEmployees" -> p.fullTimeEmployees.map(value => JsString(value)).getOrElse(JsNull),
        "phone" -> p.phone.map(value => JsString(value)).getOrElse(JsNull),
        "address" -> p.address.map(value => JsString(value)).getOrElse(JsNull),
        "city" -> p.city.map(value => JsString(value)).getOrElse(JsNull),
        "state" -> p.state.map(value => JsString(value)).getOrElse(JsNull),
        "zip" -> p.zip.map(value => JsString(value)).getOrElse(JsNull),
        "dcfDiff" -> p.dcfDiff.map(value => JsNumber(value)).getOrElse(JsNull),
        "dcf" -> p.dcf.map(value => JsNumber(value)).getOrElse(JsNull),
        "image" -> p.image.map(value => JsString(value)).getOrElse(JsNull),
        "ipoDate" -> p.ipoDate.map(value => JsString(value)).getOrElse(JsNull),
        "defaultImage" -> p.defaultImage.map(value => JsBoolean(value)).getOrElse(JsNull),
        "isEtf" -> p.isEtf.map(value => JsBoolean(value)).getOrElse(JsNull),
        "isActivelyTrading" -> p.isActivelyTrading.map(value => JsBoolean(value)).getOrElse(JsNull),
        "insertTime" -> JsNull
      )

    def read(value: JsValue) = {
      import spray.json._
      val values: Seq[JsValue] = value.asJsObject.getFields("id", "symbol", "price", "beta", "volAvg", "mktCap", "lastDiv", "range", "changes", "companyName", "currency", "cik", "isin", "cusip", "exchange", "exchangeShortName", "industry", "website", "description", "ceo", "sector", "country", "fullTimeEmployees", "phone", "address", "city", "state", "zip", "dcfDiff", "dcf", "image", "ipoDate", "defaultImage", "isEtf", "isActivelyTrading", "insertTime")
      values match {
        case JsNumber(id) +: JsString(symbol) +: (JsNumber(price) +: (JsNumber(beta) +: (JsNumber(volAvg) +: (JsNumber(mktCap) +: (JsNumber(lastDiv) +:
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
            JsBoolean(isActivelyTrading), JsNull)))))))))))))) =>
        {
          //val zipO = if (zip == null) "" else zip
          new ProfileDB(Some(id.toLong), Some(symbol), Some(price.toDouble), Some(beta.toDouble), Some(volAvg.toLong), Some(mktCap.toLong), Some(lastDiv.toDouble), Some(range), Some(changes.toDouble),
            Some(companyName), Some(currency), Some(cik), Some(isin), Some(cusip), Some(exchange), Some(exchangeShortName), Some(industry),
            Some(website), Some(description), Some(ceo), Some(sector), Some(country), Some(fullTimeEmployees), Some(phone), Some(address),
            Some(city), Some(state), Some(zip), Some(dcfDiff.toDouble), Some(dcf.toDouble), Some(image), Some(ipoDate), Some(defaultImage), Some(isEtf), Some(isActivelyTrading), null)
        }
        case ex => throw new DeserializationException(ex.toString())
      }
    }
  }

}
