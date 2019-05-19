import stryker4s.testutil.Stryker4sSuite
import stryker4s.scalatest.FileUtil
import stryker4s.config.ConfigReader
import stryker4s.command.config.ProcessRunnerConfig
import pureconfig.generic.auto._
import org.scalatest.EitherValues
import stryker4s.run.process.Command
import pureconfig.error.ConfigReaderFailures
import pureconfig.error.ConvertFailure
import pureconfig.error.KeyNotFound
import pureconfig.ConfigCursor

class ProcessConfigReaderTest extends Stryker4sSuite with EitherValues {
  describe("ProcessConfig") {
    it("should read a process config") {
      val confPath = FileUtil.getResource("config/filledProcess.conf")

      val result = ConfigReader.readConfigOfType[ProcessRunnerConfig](confPath)

      result.right.value.testRunner should equal(Command("gradle", "test"))
    }

    it("should read an empty config to errors") {
      val confPath = FileUtil.getResource("config/empty.conf")

      val result = ConfigReader.readConfigOfType[ProcessRunnerConfig](confPath)

      result.left.value should matchPattern {
        case ConfigReaderFailures(ConvertFailure(KeyNotFound("test-runner", _), _, _), _) =>
      }
    }
  }
}
