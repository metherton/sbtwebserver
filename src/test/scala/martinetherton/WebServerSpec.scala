package martinetherton

import org.scalatest._

class WebServerSpec extends FlatSpec with Matchers {
  "The Webserver serverType" should "be correct" in {
    WebServer.serverType shouldEqual "Web"
  }
}
