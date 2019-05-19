package stryker4s.config

import better.files.File
import stryker4s.testutil.Stryker4sSuite

class ConfigTest extends Stryker4sSuite {
  describe("toHoconString") {
    it("should print toString with default values") {
      val sut = Config()

      val result = sut.toHoconString

      val expected =
        s"""base-dir="${File.currentWorkingDirectory.pathAsString.replace("\\", "\\\\")}"
           |excluded-mutations=[]
           |mutate=[
           |    "**/main/scala/**/*.scala"
           |]
           |reporters=[
           |    console,
           |    html
           |]
           |thresholds {
           |    break=0
           |    high=80
           |    low=60
           |}
           |""".stripMargin
      result.toString should equal(expected.toString)
    }

    it("should print toString with changed values") {
      val filePaths = List("**/main/scala/**/Foo.scala", "**/main/scala/**/Bar.scala")
      val sut = Config(filePaths,
                       File("tmp"),
                       reporters = Seq(HtmlReporterType),
                       excludedMutations = ExcludedMutations(Set("BooleanLiteral")))

      val result = sut.toHoconString

      val expected =
        s"""base-dir="${File("tmp").pathAsString.replace("\\", "\\\\")}"
           |excluded-mutations=[
           |    BooleanLiteral
           |]
           |mutate=[
           |    "**/main/scala/**/Foo.scala",
           |    "**/main/scala/**/Bar.scala"
           |]
           |reporters=[
           |    html
           |]
           |thresholds {
           |    break=0
           |    high=80
           |    low=60
           |}
           |""".stripMargin
      result.toString should equal(expected.toString)
    }
  }
}
