package stryker4s.run.process

import stryker4s.testkit.{LogMatchers, Stryker4sIOSuite}

import scala.util.Properties

class ProcessRunnerTest extends Stryker4sIOSuite with LogMatchers {
  describe("resolveRunner") {
    test("should resolve the proper runner for the current OS") {
      val result = ProcessRunner()

      if (Properties.isWin)
        assert(result.isInstanceOf[WindowsProcessRunner])
      else
        assert(result.isInstanceOf[UnixProcessRunner])
    }
  }
}
