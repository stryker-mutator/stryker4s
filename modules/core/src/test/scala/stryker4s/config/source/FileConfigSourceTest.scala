package stryker4s.config.source

import cats.effect.IO
import cats.syntax.all.*
import ciris.ConfigValue
import fansi.Underlined
import fs2.io.file.Path
import stryker4s.config.codec.Hocon
import stryker4s.config.{ExcludedMutation, Html, MutationScoreOnly}
import stryker4s.testkit.{FileUtil, LogMatchers, Stryker4sIOSuite}
import stryker4s.testutil.ExampleConfigs
import sttp.client4.UriContext

import scala.concurrent.duration.*
import scala.meta.dialects.*

class FileConfigSourceTest extends Stryker4sIOSuite with LogMatchers {
  test("should load a filled config") {
    val config = newConfigSource(ExampleConfigs.filled)

    config.baseDir.load.assertEquals(Path("/tmp/project")) *>
      config.mutate.load.assertEquals(
        Seq("bar/src/main/**/*.scala", "foo/src/main/**/*.scala", "!excluded/file.scala")
      ) *>
      config.reporters.load.map(_.loneElement).assertEquals(Html) *>
      config.excludedMutations.load.map(_.loneElement).assertEquals(ExcludedMutation("BooleanLiteral")) *>
      config.thresholdsHigh.load.assertEquals(85) *>
      config.thresholdsLow.load.assertEquals(65) *>
      config.thresholdsBreak.load.assertEquals(10) *>
      config.timeoutFactor.load.assertEquals(2.5) *>
      config.timeout.load.assertEquals(5.5.seconds) *>
      config.maxTestRunnerReuse.load.assertEquals(15.some) *>
      config.legacyTestRunner.load.assertEquals(true) *>
      config.scalaDialect.load.assertEquals(Scala212) *>
      config.concurrency.load.assertEquals(3) *>
      config.debugDebugTestRunner.load.assertEquals(true) *>
      config.debugLogTestRunnerStdout.load.assertEquals(true) *>
      config.staticTmpDir.load.assertEquals(true) *>
      config.cleanTmpDir.load.assertEquals(false) *>
      config.dashboardBaseUrl.load.assertEquals(uri"https://fakeurl.com") *>
      config.dashboardReportType.load.assertEquals(MutationScoreOnly) *>
      config.dashboardProject.load.assertEquals("someProject".some) *>
      config.dashboardVersion.load.assertEquals("someVersion".some) *>
      config.dashboardModule.load.assertEquals("someModule".some)
  }

  test("should not be able to load an empty config") {
    newConfigSource(ExampleConfigs.empty).baseDir.attempt
      .map(_.leftValue.messages.loneElement)
      .assertEquals("Missing stryker4s.base-dir in stryker4s.conf")
  }

  test("should fail on an unknown reporter") {
    newConfigSource(ExampleConfigs.wrongReporter).reporters.attempt
      .map(_.leftValue.messages.loneElement)
      .assertEquals("Unable to convert value dsadsa to reporter")
  }

  test("should load a config with unknown keys") {
    val config = newConfigSource(ExampleConfigs.overfilled)

    config.baseDir.load.assertEquals(Path("/tmp/project")) *>
      config.mutate.load.assertEquals(
        Seq("bar/src/main/**/*.scala", "foo/src/main/**/*.scala", "!excluded/file.scala")
      ) *>
      config.reporters.load.map(_.loneElement).assertEquals(Html) *>
      config.excludedMutations.load.map(_.loneElement).assertEquals(ExcludedMutation("BooleanLiteral"))
  }

  test("should filter out duplicate keys") {
    newConfigSource(ExampleConfigs.duplicateKeys).reporters.load
      .map(_.loneElement)
      .assertEquals(Html)
  }

  test("should return a failure on a misshapen excluded-mutations") {
    val config = newConfigSource(ExampleConfigs.invalidExcludedMutation)

    config.excludedMutations.attempt
      .map(_.leftValue.messages.loneElement)
      .assertEquals(
        "Invalid option 'Invalid'. Valid exclusions are 'EqualityOperator, BooleanLiteral, ConditionalExpression, LogicalOperator, StringLiteral, MethodExpression, RegularExpression' and invalid option 'StillInvalid'. Valid exclusions are 'EqualityOperator, BooleanLiteral, ConditionalExpression, LogicalOperator, StringLiteral, MethodExpression, RegularExpression'"
      )
  }

  test("should parse duration expressions") {
    newConfigSource(ExampleConfigs.timeoutDuration).timeout.load.assertEquals(6.seconds)
  }

  test("should handle missing files") {
    val path = Path("thispathdoesnotexist.conf")
    FileConfigSource
      .load[IO](path)
      .flatMap(_.baseDir.attempt)
      .map(_.leftValue.messages.loneElement)
      .assertEquals(
        s"Missing file at $path with charset UTF-8"
      )
  }

  test("fails on other errors") {
    val scalaFile = FileUtil.getResource("fileTests/emptyDir/nonscalafile")
    FileConfigSource
      .load[IO](scalaFile)
      .flatMap(_.baseDir.attempt)
      .map(_.leftValue.messages.loneElement)
      .assertEquals(
        s"error reading $scalaFile: 'String: 2: Key 'totally not a scala file' may not be followed by token: end of file'"
      )
  }

  describe("logs") {
    test("should log where the config is read from") {
      val path = "thispathdoesnotexist.conf"
      for {
        configSource <- FileConfigSource.load[IO](Path(path))
        _ <- configSource.baseDir.attempt
      } yield assertLoggedDebug(s"Attempting to read config from ${Underlined.On(path)}")
    }
  }

  def newConfigSource(hocon: Hocon.HoconAt): FileConfigSource[IO] = new FileConfigSource[IO](
    ConfigValue.default(hocon)
  )
}
