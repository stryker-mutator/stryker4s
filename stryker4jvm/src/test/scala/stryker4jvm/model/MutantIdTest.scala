package stryker4jvm.model

class MutantIdTest extends Stryker4jvmSuite {
  describe("MutantId") {
    it("should have a toString that returns a number") {
      MutantId(1234).toString shouldBe "1234"
      MutantId(-1).toString shouldBe "-1"
    }
  }
}
