package martinetherton

import scala.io.Source

class ApiControllerSpec extends UnitSpec {

  behavior of "ApiController"

  it should "get information from API" in {

    val json = Source.fromURL("https://financialmodelingprep.com/api/v3/quote/AAPL,FB?apikey=0a314c85fe75ed860b38f1d1b4c2bdd2").mkString

    print(json)

  }

}
