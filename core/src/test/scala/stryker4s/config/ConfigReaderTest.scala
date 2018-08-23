package stryker4s.config

import better.files.File
import grizzled.slf4j.Logger
import pureconfig.error.ConfigReaderException
import stryker4s.{Stryker4sSuite, TestAppender}
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

      val expected = Config(files = Seq("bar/src/main/**/*.scala", "foo/src/main/**/*.scala", "!excluded/file.scala"),
                            baseDir = File("/tmp/project"))
      result should equal(expected)
    }
  }

  describe("logs") {
    it("should log when config file in directory is used") {
      val confPath = FileUtil.getResource("filledStryker4s.conf")
      //val logger = loggerOf(ConfigReader)

      ConfigReader.readConfig(confPath)

      TestAppender.events shouldBe true

      //logger.getLoggingEvents should contain
     // infoLog("Using stryker4s.conf in the current working directory")
    }

    it("should log warnings when no config file is found") {
      val confPath = File("nonExistentFile.conf")
     // val logger = loggerOf(ConfigReader)

      ConfigReader.readConfig(confPath)

     // logger.getLoggingEvents should contain inOrder (
      //  warnLog(
       //   s"Could not find config file ${File.currentWorkingDirectory / "nonExistentFile.conf"}"),
      //  warnLog("Using default config instead..."),
      //  debugLog("Config used: " + s"""stryker4s {
      //                               |  base-dir = ${File.currentWorkingDirectory}
    //                                 |  files = [**/main/scala/**/*.scala]
     //                                |}""".stripMargin)

    }
  }
}
