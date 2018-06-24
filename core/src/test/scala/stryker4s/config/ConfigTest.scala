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
           |  base-dir = ${File.currentWorkingDirectory}
           |  files = [**/main/scala/**/*.scala]
           |  excluded-files = []
           |}""".stripMargin
      result.toString should equal(expected.toString)
    }

    it("should print toString with changed values") {
      val filePaths = List("**/main/scala/**/Foo.scala", "**/main/scala/**/Bar.scala")
      val excludedFilesPaths = List("**/main/**/Exclude.scala")
      val sut = Config(filePaths, excludedFilesPaths, File("tmp"))

      val result = sut.toString

      val expected =
        s"""stryker4s {
           |  base-dir = ${File("tmp")}
           |  files = [**/main/scala/**/Foo.scala, **/main/scala/**/Bar.scala]
           |  excluded-files = [**/main/**/Exclude.scala]
           |}""".stripMargin
      result.toString should equal(expected.toString)
    }
  }
}
