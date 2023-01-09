package stryker4jvm.command.config

import org.scalatest.EitherValues
import pureconfig.error.{ConfigReaderFailures, ConvertFailure, KeyNotFound}
import pureconfig.generic.auto.*
import stryker4jvm.config.ConfigReader
import stryker4jvm.run.process.Command
import stryker4jvm.scalatest.LogMatchers
import stryker4jvm.testutil.{ExampleConfigs, Stryker4jvmSuite}

class ProcessConfigReaderTest extends Stryker4jvmSuite with LogMatchers with EitherValues {
  describe("ProcessConfig") {
    it("should read a process config") {
      val confPath = ExampleConfigs.filledProcess

      val result = ConfigReader.readConfigOfType[ProcessRunnerConfig](confPath).getOrElse(fail())

      result.testRunner should equal(Command("gradle", "test"))
    }

    it("should read an empty config to errors") {
      val confPath = ExampleConfigs.emptyStryker4s

      val result = ConfigReader.readConfigOfType[ProcessRunnerConfig](confPath)

      result.left.value should matchPattern {
        case ConfigReaderFailures(ConvertFailure(KeyNotFound("test-runner", _), _, _), _*) =>
      }
    }
  }
}
