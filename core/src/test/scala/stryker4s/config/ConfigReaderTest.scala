package stryker4s.config

import better.files.File
import pureconfig.error.ConfigReaderException
import stryker4s.Stryker4sSuite
import stryker4s.scalatest.FileUtil

class ConfigReaderTest extends Stryker4sSuite {

  describe("loadConfig") {
    it("should load default config with a nonexistent conf file") {
      val confPath = File("nonExistentFile.conf")

      val result = ConfigReader.readConfig(confPath)

      result should equal(Config())
    }

    it("should fail on an empty config file") {
      val confPath = FileUtil.getResource("emptyStryker4s.conf")

      lazy val result = ConfigReader.readConfig(confPath)
      val exc = the[ConfigReaderException[_]] thrownBy result

      exc.getMessage() should include("Key not found: 'stryker4s'.")
    }

    it("should load a config with customized properties") {
      val confPath = FileUtil.getResource("filledStryker4s.conf")

      val result = ConfigReader.readConfig(confPath)

      val expected =
        Config(files =
                 Seq("bar/src/main/**/*.scala", "foo/src/main/**/*.scala", "!excluded/file.scala"),
               baseDir = File("/tmp/project"))
      result should equal(expected)
    }
  }

  describe("logs") {
    it("should log when config file in directory is used") {
      val confPath = FileUtil.getResource("filledStryker4s.conf")

      ConfigReader.readConfig(confPath)

      "Using stryker4s.conf in the current working directory" shouldBe loggedAsInfo
    }

    it("should log warnings when no config file is found") {
      val confPath = File("nonExistentFile.conf")

      ConfigReader.readConfig(confPath)

      s"Could not find config file ${File.currentWorkingDirectory / "nonExistentFile.conf"}" shouldBe loggedAsWarning
      "Using default config instead..." shouldBe loggedAsWarning
      s"""Config used: stryker4s {
                                 |  base-dir = ${File.currentWorkingDirectory}
                                 |  files = [**/main/scala/**/*.scala]
                                 |}""".stripMargin shouldBe loggedAsDebug
    }
  }
}
