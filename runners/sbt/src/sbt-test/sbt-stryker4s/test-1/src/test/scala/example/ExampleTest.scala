package example

import org.scalatest.matchers.should.Matchers
import org.scalatest.funspec.AnyFunSpec

class ExampleTest extends AnyFunSpec with Matchers {
  describe("Person") {
    it("be able to drink with age 18") {
      Example.canDrink(18) should be(true)
    }
  }
}
