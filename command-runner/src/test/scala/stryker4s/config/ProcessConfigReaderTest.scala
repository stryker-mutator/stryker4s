package stryker4s.config

import org.scalatest.EitherValues
import pureconfig.error.{ConfigReaderFailures, ConvertFailure, KeyNotFound}
import stryker4s.command.config.ProcessRunnerConfig
import stryker4s.run.process.Command
import stryker4s.scalatest.FileUtil
import stryker4s.testutil.SyncStryker4sSuite
import pureconfig.generic.auto._

class ProcessConfigReaderTest extends SyncStryker4sSuite with EitherValues {
  describe("ProcessConfig") {
    it("should read a process config") {
      val confPath = FileUtil.getResource("config/filledProcess.conf")

      val result = ConfigReader.readConfigOfType[ProcessRunnerConfig](confPath).getOrElse(fail())

      result.testRunner should equal(Command("gradle", "test"))
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
