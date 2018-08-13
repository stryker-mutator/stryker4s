package stryker4s.config

import better.files.File
import stryker4s.Stryker4sSuite

class ConfigTest extends Stryker4sSuite {
  describe("toString") {
    it("should print toString with default values") {
      val sut = Config()

      val result = sut.toString

      val expected =
        s"""stryker4s {
           |  base-dir: "${File.currentWorkingDirectory}"
           |  files: ["**/main/scala/**/*.scala"]
           |  test-runner: {
           |    command-runner: {
           |      command: "sbt test"
           |    }
           |  }
           |}""".stripMargin
      result.toString should equal(expected.toString)
    }

    it("should print toString with changed values") {
      val filePaths = List("**/main/scala/**/Foo.scala", "**/main/scala/**/Bar.scala")
      val sut =
        Config(filePaths, File("tmp"), testRunner = CommandRunner("mvn clean test"))

      val result = sut.toString

      val expected =
        s"""stryker4s {
           |  base-dir: "${File("tmp")}"
           |  files: ["**/main/scala/**/Foo.scala", "**/main/scala/**/Bar.scala"]
           |  test-runner: {
           |    command-runner: {
           |      command: "mvn clean test"
           |    }
           |  }
           |}""".stripMargin
      result.toString should equal(expected.toString)
    }
  }
}
