package example

import org.scalatest.matchers.should.Matchers
import org.scalatest.funspec.AnyFunSpec

class IgnoreMeTest extends AnyFunSpec with Matchers {
  describe("Person") {
    it("not be able to drink with age 16") {
      fail("This test should never be run because of the test-filter")
    }
  }
}
