package stryker4s.config

import pureconfig.ConfigSource
import pureconfig.error.{ConfigReaderFailures, ConvertFailure, KeyNotFound}
import pureconfig.generic.auto.*
import stryker4s.command.config.ProcessRunnerConfig
import stryker4s.run.process.Command
import stryker4s.testkit.{LogMatchers, Stryker4sSuite}

class ProcessConfigReaderTest extends Stryker4sSuite with LogMatchers {
  describe("ProcessConfig") {
    test("should read a process config") {
      val confPath = filledProcess

      val result = ConfigReader.readConfigOfType[ProcessRunnerConfig](confPath).value

      assertEquals(result.testRunner, Command("gradle", "test"))
    }

    test("should read an empty config to errors") {
      val confPath = emptyStryker4s

      val result = ConfigReader.readConfigOfType[ProcessRunnerConfig](confPath)

      result.leftValue match {
        case ConfigReaderFailures(ConvertFailure(KeyNotFound(key, _), _, _), _*) => assertEquals(key, "test-runner")
        case _ => fail(s"Expected KeyNotFound, but got $result")
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
