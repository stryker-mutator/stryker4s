package stryker4s.run.process

import stryker4s.testutil.SyncStryker4sSuite

class ProcessRunnerTest extends SyncStryker4sSuite {
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
