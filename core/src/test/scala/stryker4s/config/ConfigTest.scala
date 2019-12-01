package stryker4s.config

import better.files.File
import stryker4s.testutil.Stryker4sSuite

class ConfigTest extends Stryker4sSuite {
  describe("toHoconString") {
    it("should print toString with default values") {
      val sut = Config.default

      val result = Config.toHoconString(sut)

      val expected =
        s"""base-dir="${File.currentWorkingDirectory.pathAsString.replace("\\", "\\\\")}"
           |dashboard {
           |    base-url="https://dashboard.stryker-mutator.io"
           |    report-type=full
           |}
           |excluded-mutations=[]
           |mutate=[
           |    "**/main/scala/**.scala"
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
      val sut = Config(
        filePaths,
        File("tmp"),
        reporters = Set(Html),
        excludedMutations = ExcludedMutations(Set("BooleanLiteral")),
        dashboard = DashboardOptions(
          baseUrl = "https://fakeurl.com",
          reportType = MutationScoreOnly,
          project = Some("someProject"),
          version = Some("someVersion"),
          module = Some("someModule")
        )
      )

      val result = Config.toHoconString(sut)

      val expected =
        s"""base-dir="${File("tmp").pathAsString.replace("\\", "\\\\")}"
           |dashboard {
           |    base-url="https://fakeurl.com"
           |    module=someModule
           |    project=someProject
           |    report-type=mutation-score-only
           |    version=someVersion
           |}
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
