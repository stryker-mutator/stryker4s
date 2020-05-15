package example

import org.scalatest.matchers.should.Matchers
import org.scalatest.funspec.AnyFunSpec

class IgnoreMeTest extends AnyFunSpec with Matchers {
  describe("Person") {
    it("not be able to drink with age 16") {
      Example.canDrink(16) should be(false)
    }
  }
}
