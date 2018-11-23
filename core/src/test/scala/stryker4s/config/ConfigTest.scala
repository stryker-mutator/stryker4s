package stryker4s.config

import better.files.File
import stryker4s.Stryker4sSuite
import stryker4s.mutants.Exclusions

class ConfigTest extends Stryker4sSuite {
  describe("toHoconString") {
    it("should print toString with default values") {
      val sut = Config()

      val result = sut.toHoconString

      val expected =
        s"""base-dir="${File.currentWorkingDirectory.pathAsString.replace("\\", "\\\\")}"
           |excluded-mutations=[]
           |log-level=INFO
           |mutate=[
           |    "**/main/scala/**/*.scala"
           |]
           |reporters=[
           |    console
           |]
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
      val sut = Config(filePaths,
                       File("tmp"),
                       testRunner = CommandRunner("mvn", "clean test"),
                       excludedMutations = Exclusions(Set("BooleanSubstitution")))

      val result = sut.toHoconString

      val expected =
        s"""base-dir="${File("tmp").pathAsString.replace("\\", "\\\\")}"
           |excluded-mutations=[
           |    BooleanSubstitution
           |]
           |log-level=INFO
           |mutate=[
           |    "**/main/scala/**/Foo.scala",
           |    "**/main/scala/**/Bar.scala"
           |]
           |reporters=[
           |    console
           |]
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
