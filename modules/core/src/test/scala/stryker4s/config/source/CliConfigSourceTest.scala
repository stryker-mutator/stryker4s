package stryker4s.config.source

import cats.effect.IO
import cats.syntax.all.*
import ciris.ConfigValue
import fs2.io.file.Path
import munit.Location
import stryker4s.config.*
import stryker4s.testkit.Stryker4sIOSuite
import sttp.client3.*

import scala.concurrent.duration.*
import scala.meta.dialects

class CliConfigSourceTest extends Stryker4sIOSuite {

  test("should load a filled config") {
    parseArg(_.baseDir, "--base-dir=/tmp/project").assertEquals(Path("/tmp/project")) *>
      parseArg(_.testFilter, "--test-filter=foo").assertEquals(Seq("foo")) *>
      parseArg(
        _.mutate,
        "--mutate=bar/src/main/**/*.scala",
        "--mutate",
        "foo/src/main/**/*.scala",
        "--mutate",
        "!excluded/file.scala"
      ).assertEquals(
        Seq("bar/src/main/**/*.scala", "foo/src/main/**/*.scala", "!excluded/file.scala")
      ) *>
      parseArg(_.reporters, "--reporters=Html").map(_.loneElement).assertEquals(Html) *>
      parseArg(_.files, "--files", "foo").assertEquals(Seq("foo")) *>
      parseArg(_.excludedMutations, "--excluded-mutations=BooleanLiteral")
        .map(_.loneElement)
        .assertEquals(ExcludedMutation("BooleanLiteral")) *>
      parseArg(_.thresholdsHigh, "--thresholds.high=85").assertEquals(85) *>
      parseArg(_.thresholdsLow, "--thresholds.low=65").assertEquals(65) *>
      parseArg(_.thresholdsBreak, "--thresholds.break=10").assertEquals(10) *>
      parseArg(_.dashboardBaseUrl, "--dashboard.base-url=https://fakeurl.com").assertEquals(uri"https://fakeurl.com") *>
      parseArg(_.dashboardReportType, "--dashboard.report-type=MutationScoreOnly").assertEquals(MutationScoreOnly) *>
      parseArg(_.dashboardProject, "--dashboard.project=someProject").assertEquals("someProject".some) *>
      parseArg(_.dashboardVersion, "--dashboard.version=someVersion").assertEquals("someVersion".some) *>
      parseArg(_.dashboardModule, "--dashboard.module=someModule").assertEquals("someModule".some) *>
      parseArg(_.timeout, "--timeout=5s").assertEquals(5.seconds) *>
      parseArg(_.timeoutFactor, "--timeout-factor=2.5").assertEquals(2.5) *>
      parseArg(_.maxTestRunnerReuse, "--max-test-runner-reuse=15").assertEquals(15.some) *>
      parseArg(_.legacyTestRunner, "--legacy-test-runner").assertEquals(true) *>
      parseArg(_.scalaDialect, "--scala-dialect=Scala212").assertEquals(dialects.Scala212) *>
      parseArg(_.concurrency, "--concurrency=3").assertEquals(3) *>
      parseArg(_.debugDebugTestRunner, "--debug-test-runner").assertEquals(true) *>
      parseArg(_.debugLogTestRunnerStdout, "--log-test-runner-stdout").assertEquals(true) *>
      parseArg(_.staticTmpDir, "--static-tmp-dir").assertEquals(true) *>
      parseArg(_.cleanTmpDir, "--clean-tmp-dir").assertEquals(true) *>
      parseArg(_.testRunnerCommand, "--test-runner-command=sbt").assertEquals("sbt") *>
      parseArg(_.testRunnerArgs, "--test-runner-args", "test").assertEquals("test") *>
      parseArg(_.openReport, "--open-report").assertEquals(true) *>
      parseArg(_.showHelpMessage, "--help").map(_.value).asserting { helpMsg =>
        assert(helpMsg.startsWith("Usage: stryker4s [options]"), helpMsg)
        assert(helpMsg.contains("Stryker4s - A mutation testing tool for Scala"), helpMsg)

      }
  }

  test("should parse short arguments") {
    parseArg(
      _.mutate,
      "-m",
      "bar/src/main/**/*.scala",
      "-m",
      "!excluded/file.scala"
    ).assertEquals(
      Seq("bar/src/main/**/*.scala", "!excluded/file.scala")
    ) *>
      parseArg(_.testFilter, "-t", "foo").assertEquals(Seq("foo")) *>
      parseArg(_.baseDir, "-b", "/tmp/project").assertEquals(Path("/tmp/project")) *>
      parseArg(_.reporters, "-r", "html").map(_.loneElement).assertEquals(Html) *>
      parseArg(_.files, "-f", "foo").assertEquals(Seq("foo")) *>
      parseArg(_.excludedMutations, "-e", "BooleanLiteral")
        .map(_.loneElement)
        .assertEquals(ExcludedMutation("BooleanLiteral")) *>
      parseArg(_.timeout, "-T", "5s").assertEquals(5.seconds) *>
      parseArg(_.timeoutFactor, "-F", "2.5").assertEquals(2.5) *>
      parseArg(_.concurrency, "-c", "3").assertEquals(3) *>
      parseArg(_.openReport, "-o").assertEquals(true)

  }

  test("fails on unknown arguments") {
    parseArgAttempt(_.baseDir, "--unknown-argument=foo")
      .map(_.leftValue.messages.loneElement)
      .assertEquals("Missing unknown option --unknown-argument=foo") *>
      parseArgAttempt(_.baseDir)
        .map(_.leftValue.messages.loneElement)
        .assertEquals("Missing baseDir option")
  }

  test("should handle multiple arguments") {
    parseArg(_.mutate, "--mutate", "foo", "--files", "bar").assertEquals(Seq("foo"))
  }

  private def parseArg[T](fn: CliConfigSource[IO] => ConfigValue[IO, T], args: String*)(implicit loc: Location) =
    parseArgAttempt(fn, args*).map(_.value)

  private def parseArgAttempt[T](
      fn: CliConfigSource[IO] => ConfigValue[IO, T],
      args: String*
  ) = {
    val cs = new CliConfigSource[IO](args)
    fn(cs).attempt
  }

}
