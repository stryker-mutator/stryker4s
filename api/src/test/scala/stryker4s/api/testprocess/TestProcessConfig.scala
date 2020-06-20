package stryker4s.api.testprocess

import stryker4s.testutil.Stryker4sSuite

class TestProcessConfigTest extends Stryker4sSuite {
  describe("fromArgs") {
    it("should resolve a port") {
      val args = Seq("--port=13337")
      val result = TestProcessConfig.fromArgs(args).value

      result shouldBe TestProcessConfig(port = 13337)
    }
  }
}
