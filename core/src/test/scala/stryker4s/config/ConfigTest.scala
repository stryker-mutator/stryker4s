package stryker4s.config

import better.files.File
import stryker4s.Stryker4sSuite

class ConfigTest extends Stryker4sSuite {
  describe("toHoconString") {
    it("should print toString with default values") {
      val sut = Config()

      val result = sut.toHoconString

      val expected =
        s"""base-dir="${File.currentWorkingDirectory.pathAsString.replace("\\", "\\\\")}"
           |files=[
           |    "**/main/scala/**/*.scala"
           |]
           |log-level=INFO
           |test-runner {
           |    args=test
           |    command=sbt
           |    type=commandrunner
           |}
           |""".stripMargin
      result.toString should equal(expected.toString)
    }

    it("should print toString with changed values") {
      val filePaths = List("**/main/scala/**/Foo.scala", "**/main/scala/**/Bar.scala")
      val sut =
        Config(filePaths, File("tmp"), testRunner = CommandRunner("mvn", "clean test"))

      val result = sut.toHoconString

      val expected =
        s"""base-dir="${File("tmp").pathAsString.replace("\\", "\\\\")}"
           |files=[
           |    "**/main/scala/**/Foo.scala",
           |    "**/main/scala/**/Bar.scala"
           |]
           |log-level=INFO
           |test-runner {
           |    args="clean test"
           |    command=mvn
           |    type=commandrunner
           |}
           |""".stripMargin
      result.toString should equal(expected.toString)
    }
  }
}
