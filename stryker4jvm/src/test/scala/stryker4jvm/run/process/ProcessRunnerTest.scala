package stryker4jvm.run.process

import stryker4jvm.scalatest.LogMatchers
import stryker4jvm.testutil.Stryker4jvmIOSuite

class ProcessRunnerTest extends Stryker4jvmIOSuite with LogMatchers {
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
