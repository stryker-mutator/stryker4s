package stryker4s.config

import org.scalatest.EitherValues
import pureconfig.ConfigSource
import pureconfig.error.{ConfigReaderFailures, ConvertFailure, KeyNotFound}
import pureconfig.generic.auto.*
import stryker4s.command.config.ProcessRunnerConfig
import stryker4s.run.process.Command
import stryker4s.scalatest.LogMatchers
import stryker4s.testutil.Stryker4sSuite

class ProcessConfigReaderTest extends Stryker4sSuite with LogMatchers with EitherValues {
  describe("ProcessConfig") {
    it("should read a process config") {
      val confPath = filledProcess

      val result = ConfigReader.readConfigOfType[ProcessRunnerConfig](confPath).getOrElse(fail())

      result.testRunner should equal(Command("gradle", "test"))
    }

    it("should read an empty config to errors") {
      val confPath = emptyStryker4s

      val result = ConfigReader.readConfigOfType[ProcessRunnerConfig](confPath)

      result.left.value should matchPattern {
        case ConfigReaderFailures(ConvertFailure(KeyNotFound("test-runner", _), _, _), _*) =>
      }
    }
  }

  def filledProcess = ConfigSource.string("""stryker4s {
                                            |  test-runner {
                                            |    command = "gradle"
                                            |    args="test"
                                            |  }
                                            |}""".stripMargin)

  def emptyStryker4s = ConfigSource.string("stryker4s {}")

}
