package stryker4s.run.process

import stryker4s.scalatest.LogMatchers
import stryker4s.testutil.Stryker4sIOSuite

import scala.util.Properties

class ProcessRunnerTest extends Stryker4sIOSuite with LogMatchers {
  describe("resolveRunner") {
    it("should resolve the proper runner for the current OS") {
      val result = ProcessRunner()

      if (Properties.isWin)
        result shouldBe a[WindowsProcessRunner]
      else
        result shouldBe a[UnixProcessRunner]
    }
  }
}
