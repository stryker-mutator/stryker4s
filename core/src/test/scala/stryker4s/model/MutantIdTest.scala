package stryker4s.model

import stryker4s.testutil.Stryker4sSuite

class MutantIdTest extends Stryker4sSuite {
  describe("MutantId") {
    it("should have a toString that returns a number") {
      MutantId(1234).toString shouldBe "1234"
      MutantId(-1).toString shouldBe "-1"
    }
  }
}
