package martinetherton

import io.cucumber.scala.{EN, ScalaDsl}

class ApiStepDefinitions extends ScalaDsl with EN {

  Given("""keepalive is called""") { () =>
    // Write code here that turns the phrase above into concrete actions
    throw new io.cucumber.scala.PendingException()
  }

  Then("""response should be {int}""") { (int1: Int) =>
    // Write code here that turns the phrase above into concrete actions
    throw new io.cucumber.scala.PendingException()
  }


}
