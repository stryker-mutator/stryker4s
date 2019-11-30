package example

import org.scalatest.matchers.should.Matchers
import org.scalatest.funspec.AnyFunSpec

class ExampleTest extends AnyFunSpec with Matchers {
  "Person" should "be able to drink with age 18" in {
    Example.canDrink(18) should be(true)
  }
}
