package stryker4s.run.process

import stryker4s.scalatest.LogMatchers
import stryker4s.testutil.Stryker4sIOSuite

class ProcessRunnerTest extends Stryker4sIOSuite with LogMatchers {
  describe("resolveRunner") {
    it("should resolve the proper runner for the current OS") {
      val os = sys.props("os.name")
      val result = ProcessRunner()

      if (os.toLowerCase.contains("windows"))
        result shouldBe a[WindowsProcessRunner]
      else
        result shouldBe a[UnixProcessRunner]
    }
  }
}
