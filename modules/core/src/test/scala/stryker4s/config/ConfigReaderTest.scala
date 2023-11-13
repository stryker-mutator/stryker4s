package stryker4s.config

import cats.syntax.all.*
import fansi.Color.Yellow
import fansi.Underlined
import fs2.io.file.Path
import munit.Location
import pureconfig.error.{CannotConvert, ConfigReaderException, ConfigReaderFailures, ConvertFailure, FailureReason}
import pureconfig.generic.auto.*
import pureconfig.module.sttp.reader
import pureconfig.{ConfigObjectSource, ConfigSource}
import stryker4s.config.Config.*
import stryker4s.testkit.{LogMatchers, Stryker4sSuite}
import stryker4s.testutil.ExampleConfigs
import sttp.client3.UriContext

import scala.concurrent.duration.*
import scala.meta.dialects.*

class ConfigReaderTest extends Stryker4sSuite with LogMatchers {
  describe("loadConfig") {
    test("should load stryker4s by type") {
      val configSource = ExampleConfigs.filled

      ConfigReader.readConfigOfType[Config](configSource) match {
        case Left(errors) => fail(errors.toList.mkString(","))
        case Right(config) =>
          assertEquals(config.baseDir, Path("/tmp/project").absolute)
          assertEquals(config.mutate, Seq("bar/src/main/**/*.scala", "foo/src/main/**/*.scala", "!excluded/file.scala"))
          assertEquals(config.reporters.loneElement, Html)
          assertEquals(config.excludedMutations, Set("BooleanLiteral"))
          assertEquals(config.thresholds, Thresholds(high = 85, low = 65, break = 10))
          assertEquals(config.timeoutFactor, 2.5)
          assertEquals(config.timeout, 5.5.seconds)
          assertEquals(config.maxTestRunnerReuse.value, 15)
          assert(config.legacyTestRunner)
          assertEquals(config.scalaDialect, Scala212)
          assertEquals(config.concurrency, 3)
          assertEquals(config.debug, DebugOptions(true, true))
          assert(config.staticTmpDir)
          assertEquals(config.cleanTmpDir, false)
      }
    }

    test("should not be able to load a empty config") {
      val configSource = ExampleConfigs.empty

      ConfigReader.readConfigOfType[Config](configSource) match {
        case Left(error) => assertEquals(error.toList.map(a => a.description), List("Key not found: 'stryker4s'."))
        case Right(_)    => fail("Config was read successfully which should not be the case.")
      }
    }

    test("should load default config with a nonexistent conf file") {
      val configSource = ExampleConfigs.nonExistentFile

      val result = ConfigReader.readConfig(configSource)

      assertEquals(result.baseDir, Path("").absolute)
      assert(result.mutate.isEmpty)
      assert(result.files.isEmpty)
      assertSameElements(result.reporters, Set(Html, Console))
      assertEquals(result.thresholds, Thresholds())
      assertEquals(result.maxTestRunnerReuse, none)
      assertEquals(
        result.dashboard,
        DashboardOptions(
          baseUrl = uri"https://dashboard.stryker-mutator.io",
          reportType = Full,
          project = none,
          version = none,
          module = none
        )
      )
      assertEquals(result.scalaDialect, Scala213Source3)
      assertEquals(result.debug, DebugOptions(false, false))
      assertEquals(result.staticTmpDir, false)
      assert(result.cleanTmpDir)
    }

    test("should fail on an empty config file") {
      val configSource = ExampleConfigs.empty

      lazy val result = ConfigReader.readConfig(configSource)
      val exc = intercept[ConfigReaderException[?]](result)

      assertLoggedError("Failures in reading config:")
      assert(exc.getMessage().contains("Key not found: 'stryker4s'."))
    }

    test("should fail on an unknown reporter") {
      val configSource = ExampleConfigs.wrongReporter

      lazy val result = ConfigReader.readConfig(configSource)
      val exc = intercept[ConfigReaderException[?]](result)

      assert(exc.getMessage().contains("Cannot convert configuration"))
    }

    test("should load a config with unknown keys") {
      val configSource = ExampleConfigs.overfilled

      lazy val config = ConfigReader.readConfig(configSource)

      assertEquals(config.baseDir, Path("/tmp/project").absolute)
      assertEquals(config.mutate, Seq("bar/src/main/**/*.scala", "foo/src/main/**/*.scala", "!excluded/file.scala"))
      assertEquals(config.reporters.loneElement, Html)
      assertEquals(config.excludedMutations, Set("BooleanLiteral"))
    }

    test("should load a config with customized properties") {
      val configSource = ExampleConfigs.filled

      val result = ConfigReader.readConfig(configSource)

      assertEquals(result.baseDir, Path("/tmp/project").absolute)
      assertEquals(result.mutate, Seq("bar/src/main/**/*.scala", "foo/src/main/**/*.scala", "!excluded/file.scala"))
      assertEquals(result.reporters.loneElement, Html)
      assertEquals(result.excludedMutations, Set("BooleanLiteral"))
      assertEquals(
        result.dashboard,
        DashboardOptions(
          baseUrl = uri"https://fakeurl.com",
          reportType = MutationScoreOnly,
          project = "someProject".some,
          version = "someVersion".some,
          module = "someModule".some
        )
      )
    }

    test("should filter out duplicate keys") {
      val configSource = ExampleConfigs.duplicateKeys

      val result = ConfigReader.readConfig(configSource)

      assertEquals(result.reporters.loneElement, Html)
    }

    test("should return a failure on a misshapen excluded-mutations") {
      val configSource = ExampleConfigs.invalidExcludedMutation

      val exc = intercept[ConfigReaderException[?]](ConfigReader.readConfig(configSource))

      val head = exc.failures.head
      assert(head.isInstanceOf[ConvertFailure])
      val errorMessage =
        "Cannot convert 'Invalid, StillInvalid, BooleanLiteral' to excluded-mutations: invalid option(s) 'Invalid, StillInvalid'. Valid exclusions are 'EqualityOperator, BooleanLiteral, ConditionalExpression, LogicalOperator, StringLiteral, MethodExpression, RegularExpression'."
      assertLoggedError(errorMessage)
    }

    test("should parse duration expressions") {
      val configSource = ExampleConfigs.timeoutDuration

      val result = ConfigReader.readConfig(configSource)

      assertEquals(result.timeout, 6.seconds)
    }
  }

  describe("logs") {
    test("should log where the config is read from") {
      val configSource = ExampleConfigs.filled

      ConfigReader.readConfig(configSource)

      assertLoggedInfo(s"Attempting to read config from ${Underlined.On("stryker4s.conf")}")
    }

    test("should log warnings when no config file is found") {
      val configSource = ExampleConfigs.nonExistentFile

      ConfigReader.readConfig(configSource)

      val absolutePath = Path("nonExistentFile.conf").absolute
      assertLoggedWarn(s"Could not find config file $absolutePath")
      assertLoggedWarn("Using default config instead...")
      assertLoggedDebug(s"Config used: ${Config.default}")
    }

    test("should log warnings when unknown keys are used") {
      val configSource = ExampleConfigs.overfilled

      ConfigReader.readConfig(configSource)

      assertLoggedWarn(
        s"""|The following configuration key(s) are not used, they could stem from an older stryker4s version: '${Yellow(
             "other-unknown-key"
           )}, ${Yellow("unknown-key")}'.
            |Please check the documentation at https://stryker-mutator.io/docs/stryker4s/configuration for available options.""".stripMargin
      )
    }
  }

  describe("ConfigReaderImplicits") {
    describe("Thresholds") {
      val testValues = List(
        "empty=true" -> Thresholds().asRight,
        "high=85, low=65, break=10" -> Thresholds(high = 85, low = 65, break = 10).asRight,
        "high=30, low=30" -> Thresholds(high = 30, low = 30).asRight,
        "low=30, break=29" -> Thresholds(low = 30, break = 29).asRight,
        "high=100" -> Thresholds(high = 100).asRight,
        "high=-1" -> CannotConvert("-1", "thresholds.high", "must be a percentage 0-100").asLeft,
        "low=-1" -> CannotConvert("-1", "thresholds.low", "must be a percentage 0-100").asLeft,
        "break=-1" -> CannotConvert("-1", "thresholds.break", "must be a percentage 0-100").asLeft,
        "high=101" -> CannotConvert("101", "thresholds.high", "must be a percentage 0-100").asLeft,
        "low=101" -> CannotConvert("101", "thresholds.low", "must be a percentage 0-100").asLeft,
        "break=101" -> CannotConvert("101", "thresholds.break", "must be a percentage 0-100").asLeft,
        "high=50,low=51" -> CannotConvert(
          "50",
          "thresholds.high",
          "'high' (50) must be greater than or equal to 'low' (51)"
        ).asLeft,
        "low=50,break=51" -> CannotConvert(
          "50",
          "thresholds.low",
          "'low' (50) must be greater than 'break' (51)"
        ).asLeft,
        "low=50,break=50" -> CannotConvert(
          "50",
          "thresholds.low",
          "'low' (50) must be greater than 'break' (50)"
        ).asLeft
      )

      testValues.foreach { case (config, expected) =>
        test(s"should load $config to expected result") {
          val result = ConfigSource.string(config).load[Thresholds]

          result match {
            case Right(value) => assertEquals(value, expected.value)
            case Left(ConfigReaderFailures(ConvertFailure(reason, _, _), _*)) =>
              assertEquals(reason, expected.leftValue)
            case Left(other) => fail(s"unexpected value $other")
          }
        }
      }
    }

    describe("ScalaDialect") {
      val validVersions = Map(
        "scala212" -> Scala212,
        "scala2.12" -> Scala212,
        "2.12" -> Scala212,
        "212" -> Scala212,
        "scala212source3" -> Scala212Source3,
        "scala213" -> Scala213,
        "scala2.13" -> Scala213,
        "2.13" -> Scala213,
        "213" -> Scala213,
        "2" -> Scala213,
        "scala213source3" -> Scala213Source3,
        "source3" -> Scala213Source3,
        "scala3future" -> Scala3Future,
        "future" -> Scala3Future,
        "scala30" -> Scala30,
        "scala3.0" -> Scala30,
        "3.0" -> Scala30,
        "30" -> Scala30,
        "dotty" -> Scala30,
        "scala31" -> Scala31,
        "scala3.1" -> Scala31,
        "3.1" -> Scala31,
        "31" -> Scala31,
        "scala32" -> Scala32,
        "scala3.2" -> Scala32,
        "3.2" -> Scala32,
        "32" -> Scala32,
        "scala33" -> Scala33,
        "scala3.3" -> Scala33,
        "3.3" -> Scala33,
        "33" -> Scala33,
        "scala3" -> Scala3,
        "3" -> Scala3
      )

      validVersions.foreach { case (input, expected) =>
        test(s"should parse $input to $expected") {
          ExampleConfigs.scalaDialect(input).at("stryker4s").load[Config] match {
            case Right(value) => assertEquals(value.scalaDialect, expected)
            case Left(value)  => fail(s"Expected valid parsing, got $value")
          }

        }
      }

      test("should not parse invalid scala-dialects") {
        expectConfigFailure(
          ExampleConfigs.scalaDialect("foobar"),
          CannotConvert(
            "foobar",
            "scala-dialect",
            s"Unsupported dialect. Leaving this configuration empty defaults to scala213source3 which might also work for you. Valid scalaDialects are: ${validVersions.keys
                .map("'" + _ + "'")
                .mkString(", ")}"
          )
        )
      }

      val deprecatedVersions = List("scala211", "scala2.11", "2.11", "211")

      deprecatedVersions.foreach { version =>
        test(s"should error deprecated scala-dialect $version") {
          expectConfigFailure(
            ExampleConfigs.scalaDialect(version),
            CannotConvert(
              version,
              "scala-dialect",
              s"Deprecated dialect. Leaving this configuration empty defaults to scala213source3 which might also work for you. Valid scalaDialects are: ${validVersions.keys
                  .map("'" + _ + "'")
                  .mkString(", ")}"
            )
          )
        }
      }
    }

    def expectConfigFailure(config: ConfigObjectSource, failure: FailureReason)(implicit loc: Location) =
      config.at("stryker4s").load[Config] match {
        case Left(ConfigReaderFailures(ConvertFailure(reason, _, _), _*)) =>
          assertEquals(reason.description, failure.description)
          assertEquals(reason, failure)
        case value => fail(s"Expected parsing failure but got $value")
      }
  }
}
