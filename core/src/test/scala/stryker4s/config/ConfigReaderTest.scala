package stryker4s.config

import better.files.File
import pureconfig.error.{ConfigReaderException, ConvertFailure}
import stryker4s.scalatest.{FileUtil, LogMatchers}
import stryker4s.testutil.Stryker4sSuite

class ConfigReaderTest extends Stryker4sSuite with LogMatchers {

  describe("loadConfig") {
    it("should load default config with a nonexistent conf file") {
      val confPath = File("nonExistentFile.conf")

      val result = ConfigReader.readConfig(confPath)

      result.baseDir shouldBe File.currentWorkingDirectory
      result.mutate shouldBe Seq("**/main/scala/**/*.scala")
      result.reporters.loneElement shouldBe ConsoleReporterType
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
      result.mutate shouldBe Seq("bar/src/main/**/*.scala", "foo/src/main/**/*.scala", "!excluded/file.scala")
      result.reporters should contain only (ConsoleReporterType, HtmlReporterType)
      result.excludedMutations shouldBe ExcludedMutations(Set("BooleanLiteral"))
    }

    it("should return a failure on a misshapen test runner") {
      val confPath = FileUtil.getResource("stryker4sconfs/invalidExcludedMutation.conf")

      lazy val result = ConfigReader.readConfig(confPath)
      val exc = the[ConfigReaderException[_]] thrownBy result

      val head = exc.failures.head
      head shouldBe a[ConvertFailure]
      val errorMessage =
        s"""Invalid exclusion option(s): 'Invalid, StillInvalid'
           |Valid exclusions are EqualityOperator, BooleanLiteral, ConditionalExpression, LogicalOperator, StringLiteral, MethodExpression.""".stripMargin
      errorMessage shouldBe loggedAsError
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
      // Ignored due to transitive dependency clash in sbt
      // s"Config used: ${sut.toHoconString}" shouldBe loggedAsInfo
    }
  }
}
