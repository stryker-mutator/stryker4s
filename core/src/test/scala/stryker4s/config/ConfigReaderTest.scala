package stryker4s.config

import better.files.File
import ch.qos.logback.classic.Level
import pureconfig.error.{ConfigReaderException, ConvertFailure}
import stryker4s.Stryker4sSuite
import stryker4s.run.report.ConsoleReporter
import stryker4s.scalatest.{FileUtil, LogMatchers}

class ConfigReaderTest extends Stryker4sSuite with LogMatchers {

  describe("loadConfig") {
    it("should load default config with a nonexistent conf file") {
      val confPath = File("nonExistentFile.conf")

      val result = ConfigReader.readConfig(confPath)

      result.baseDir shouldBe File.currentWorkingDirectory
      result.files shouldBe Seq("**/main/scala/**/*.scala")
      result.testRunner shouldBe an[CommandRunner]
      result.logLevel shouldBe Level.INFO
      result.reporters.head shouldBe an[ConsoleReporter]
    }

    it("should fail on an empty config file") {
      val confPath = FileUtil.getResource("stryker4sconfs/empty.conf")

      lazy val result = ConfigReader.readConfig(confPath)
      val exc = the[ConfigReaderException[_]] thrownBy result

      exc.getMessage() should include("Key not found: 'stryker4s'.")
    }

    it("should fail on an unknown reporter") {
      val confPath = FileUtil.getResource("stryker4sconfs/wrongReporter.conf")

      lazy val result = ConfigReader.readConfig(confPath)
      val exc = the[ConfigReaderException[_]] thrownBy result

      exc.getMessage() should include("Cannot convert configuration")
    }

    it("should load a config with customized properties") {
      val confPath = FileUtil.getResource("stryker4sconfs/filled.conf")

      val result = ConfigReader.readConfig(confPath)

      result.baseDir shouldBe File("/tmp/project")
      result.files shouldBe Seq("bar/src/main/**/*.scala",
                                "foo/src/main/**/*.scala",
                                "!excluded/file.scala")
      result.testRunner shouldBe an[CommandRunner]
      result.logLevel shouldBe Level.DEBUG
      result.reporters.head shouldBe an[ConsoleReporter]
    }

    it("should return a failure on a misshapen test runner") {
      val confPath = FileUtil.getResource("stryker4sconfs/wrongTestRunner.conf")

      lazy val result = ConfigReader.readConfig(confPath)
      val exc = the[ConfigReaderException[_]] thrownBy result

      val head = exc.failures.head
      head shouldBe a[ConvertFailure]
      head.description should equal(
        s"""No valid coproduct choice found for '{"args":"foo","command":"bar","type":"someOtherTestRunner"}'.""")
    }
  }

  describe("logs") {
    it("should log when config file in directory is used") {
      val confPath = FileUtil.getResource("stryker4sconfs/filled.conf")

      ConfigReader.readConfig(confPath)

      "Using stryker4s.conf in the current working directory" shouldBe loggedAsInfo
    }

    it("should log warnings when no config file is found") {
      val confPath = File("nonExistentFile.conf")

      val sut = ConfigReader.readConfig(confPath)

      s"Could not find config file ${File.currentWorkingDirectory / "nonExistentFile.conf"}" shouldBe loggedAsWarning
      "Using default config instead..." shouldBe loggedAsWarning
      s"Config used: ${sut.toHoconString}" shouldBe loggedAsInfo
    }
  }
}
