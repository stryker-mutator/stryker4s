package stryker4s.model

import stryker4s.testkit.Stryker4sSuite

class MutantIdTest extends Stryker4sSuite {
  describe("MutantId") {
    test("should have a toString that returns a number") {
      assertEquals(MutantId(1234).toString, "1234")
      assertEquals(MutantId(-1).toString, "-1")
    }
  }
}
