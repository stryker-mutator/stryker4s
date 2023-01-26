package stryker4jvm.config

import fansi.Color.Yellow
import fansi.Underlined
import fs2.io.file.Path
import org.scalactic.source.Position
import pureconfig.error.*
import pureconfig.generic.auto.*
import pureconfig.{ConfigObjectSource, ConfigSource}
import stryker4jvm.config.Config.*
import stryker4jvm.scalatest.LogMatchers
import stryker4jvm.testutil.{ExampleConfigs, Stryker4jvmSuite}
import pure.*
import stryker4jvm.core.config.LanguageMutatorConfig
import sttp.client3.UriContext

import scala.concurrent.duration.*
import scala.meta.dialects
import scala.meta.dialects.*

class ConfigReaderTest extends Stryker4jvmSuite with LogMatchers {
  describe("loadConfig") {
    it("Should read language mutator configurations") {
      val configSource = ExampleConfigs.withLanguageMutatorConfigs
      val config = ConfigReader.readConfig(configSource)
      // ConfigReader.readConfigOfType[Config](configSource) // <-- this does not compile...

      config.mutatorConfigs should have size 2

      config.mutatorConfigs.contains("scala") shouldBe true
      val scalaConfig: LanguageMutatorConfig = config.mutatorConfigs.getOrElse("scala", null)
      scalaConfig.getDialect shouldBe "2_13"
      scalaConfig.getExcludedMutations should have size 1
      scalaConfig.getExcludedMutations.contains("BooleanLiteral") shouldBe true

      config.mutatorConfigs.contains("kotlin") shouldBe true
      val kotlinConfig: LanguageMutatorConfig = config.mutatorConfigs.getOrElse("kotlin", null)
      kotlinConfig.getDialect shouldBe null
      kotlinConfig.getExcludedMutations should have size 2
      kotlinConfig.getExcludedMutations.contains("EqualityOperator") shouldBe true
      kotlinConfig.getExcludedMutations.contains("AnotherMutatorType") shouldBe true
    }

    it("should load stryker4jvm by type") {
      val configSource = ExampleConfigs.filled

      ConfigReader.readConfigOfType[Config](configSource) match {
        case Left(errors) => fail(errors.toList.mkString(","))
        case Right(config) =>
          config.baseDir shouldBe Path("/tmp/project").absolute
          config.mutate shouldBe Seq(
            "bar/src/main/**/*.scala",
            "foo/src/main/**/*.scala",
            "!excluded/file.scala",
            "koo/src/main/**/*.kt"
          )
          config.reporters.loneElement shouldBe Html
          config.mutatorConfigs.contains("scala") shouldBe true
          config.mutatorConfigs("scala").getExcludedMutations.contains("BooleanLiteral") shouldBe true
          config.mutatorConfigs("scala").getDialect shouldBe "2_13"
          config.thresholds shouldBe Thresholds(high = 85, low = 65, break = 10)
          config.timeoutFactor shouldBe 2.5
          config.timeout shouldBe 5.5.seconds
          config.maxTestRunnerReuse.value shouldBe 15
          config.legacyTestRunner shouldBe true
          config.concurrency shouldBe 3
          config.debug shouldBe DebugOptions(true, true)
          config.staticTmpDir shouldBe true
          config.cleanTmpDir shouldBe false
      }
    }

    it("should not be able to load a empty config") {
      val configSource = ExampleConfigs.empty

      ConfigReader.readConfigOfType[Config](configSource) match {
        case Left(error) => error.toList.map(a => a.description) shouldBe List("Key not found: 'stryker4jvm'.")
        case Right(_)    => fail("Config was read successfully which should not be the case.")
      }
    }

    it("should load default config with a nonexistent conf file") {
      val configSource = ExampleConfigs.nonExistentFile

      val result = ConfigReader.readConfig(configSource)

      result.baseDir shouldBe Path("").absolute
      result.mutate shouldBe empty
      result.files shouldBe empty
      result.reporters should (contain.only(Html, Console))
      result.thresholds shouldBe Thresholds()
      result.maxTestRunnerReuse shouldBe None
      result.dashboard shouldBe DashboardOptions(
        baseUrl = uri"https://dashboard.stryker-mutator.io",
        reportType = Full,
        project = None,
        version = None,
        module = None
      )
      result.mutatorConfigs.size shouldBe 0
      result.debug shouldBe DebugOptions(false, false)
      result.staticTmpDir shouldBe false
      result.cleanTmpDir shouldBe true
    }

    it("should fail on an empty config file") {
      val configSource = ExampleConfigs.empty

      lazy val result = ConfigReader.readConfig(configSource)
      val exc = the[ConfigReaderException[?]] thrownBy result

      "Failures in reading config:" shouldBe loggedAsError
      exc.getMessage() should include("Key not found: 'stryker4jvm'.")
    }

    it("should fail on an unknown reporter") {
      val configSource = ExampleConfigs.wrongReporter

      lazy val result = ConfigReader.readConfig(configSource)
      val exc = the[ConfigReaderException[?]] thrownBy result

      exc.getMessage() should include("Cannot convert configuration")
    }

    it("should load a config with unknown keys") {
      val configSource = ExampleConfigs.overfilled

      lazy val config = ConfigReader.readConfig(configSource)

      config.baseDir shouldBe Path("/tmp/project").absolute
      config.mutate shouldBe Seq(
        "bar/src/main/**/*.scala",
        "foo/src/main/**/*.scala",
        "!excluded/file.scala",
        "koo/src/main/**/*.kt"
      )
      config.reporters.loneElement shouldBe Html
      config.mutatorConfigs.contains("scala") shouldBe true
      config.mutatorConfigs("scala").getExcludedMutations.contains("BooleanLiteral") shouldBe true
      config.mutatorConfigs.contains("kotlin") shouldBe true
      config.mutatorConfigs("kotlin").getExcludedMutations.contains("EqualityOperator") shouldBe true
    }

    it("should load a config with customized properties") {
      val configSource = ExampleConfigs.filled

      val result = ConfigReader.readConfig(configSource)

      result.baseDir shouldBe Path("/tmp/project").absolute
      result.mutate shouldBe Seq(
        "bar/src/main/**/*.scala",
        "foo/src/main/**/*.scala",
        "!excluded/file.scala",
        "koo/src/main/**/*.kt"
      )
      result.reporters.loneElement shouldBe Html
      result.mutatorConfigs.contains("scala") shouldBe true
      result.mutatorConfigs("scala").getExcludedMutations.contains("BooleanLiteral") shouldBe true
      result.dashboard shouldBe DashboardOptions(
        baseUrl = uri"https://fakeurl.com",
        reportType = MutationScoreOnly,
        project = Some("someProject"),
        version = Some("someVersion"),
        module = Some("someModule")
      )
    }

    it("should filter out duplicate keys") {
      val configSource = ExampleConfigs.duplicateKeys

      val result = ConfigReader.readConfig(configSource)

      result.reporters.loneElement shouldBe Html
    }

    // Mutation types are not filtered anymore in Stryker4jvm
//    it("should return a failure on a misshapen excluded-mutations") {
//      val configSource = ExampleConfigs.invalidExcludedMutation
//
//      lazy val result = ConfigReader.readConfig(configSource)
//      val exc = the[ConfigReaderException[?]] thrownBy result
//
//      val head = exc.failures.head
//      head shouldBe a[ConvertFailure]
//      val errorMessage =
//        "Cannot convert 'Invalid, StillInvalid, BooleanLiteral' to excluded-mutations: invalid option(s) 'Invalid, StillInvalid'. Valid exclusions are 'EqualityOperator, BooleanLiteral, ConditionalExpression, LogicalOperator, StringLiteral, MethodExpression, RegularExpression'."
//      errorMessage shouldBe loggedAsError
//    }

    it("should parse duration expressions") {
      val configSource = ExampleConfigs.timeoutDuration

      val result = ConfigReader.readConfig(configSource)

      result.timeout shouldBe 6.seconds
    }
  }

  describe("logs") {
    it("should log where the config is read from") {
      val configSource = ExampleConfigs.filled

      ConfigReader.readConfig(configSource)

      s"Attempting to read config from ${Underlined.On("stryker4jvm.conf")}" shouldBe loggedAsInfo
    }

    it("should log warnings when no config file is found") {
      val configSource = ExampleConfigs.nonExistentFile

      ConfigReader.readConfig(configSource)

      val absolutePath = Path("nonExistentFile.conf").absolute
      s"Could not find config file $absolutePath" shouldBe loggedAsWarning
      "Using default config instead..." shouldBe loggedAsWarning
      s"Config used: ${Config.default}" shouldBe loggedAsDebug
    }

    it("should log warnings when unknown keys are used") {
      val configSource = ExampleConfigs.overfilled

      ConfigReader.readConfig(configSource)

      s"""|The following configuration key(s) are not used, they could stem from an older stryker4jvm version: '${Yellow(
           "other-unknown-key"
         )}, ${Yellow("unknown-key")}'.
          |Please check the documentation at https://stryker-mutator.io/docs/stryker4s/configuration for available options.""".stripMargin shouldBe loggedAsWarning
    }
  }

  describe("ConfigReaderImplicits") {
    describe("Thresholds") {
      val testValues = List(
        "empty=true" -> Thresholds(),
        "high=85, low=65, break=10" -> Thresholds(high = 85, low = 65, break = 10),
        "high=30, low=30" -> Thresholds(high = 30, low = 30),
        "low=30, break=29" -> Thresholds(low = 30, break = 29),
        "high=100" -> Thresholds(high = 100),
        "high=-1" -> CannotConvert("-1", "thresholds.high", "must be a percentage 0-100"),
        "low=-1" -> CannotConvert("-1", "thresholds.low", "must be a percentage 0-100"),
        "break=-1" -> CannotConvert("-1", "thresholds.break", "must be a percentage 0-100"),
        "high=101" -> CannotConvert("101", "thresholds.high", "must be a percentage 0-100"),
        "low=101" -> CannotConvert("101", "thresholds.low", "must be a percentage 0-100"),
        "break=101" -> CannotConvert("101", "thresholds.break", "must be a percentage 0-100"),
        "high=50,low=51" -> CannotConvert(
          "50",
          "thresholds.high",
          "'high' (50) must be greater than or equal to 'low' (51)"
        ),
        "low=50,break=51" -> CannotConvert("50", "thresholds.low", "'low' (50) must be greater than 'break' (51)"),
        "low=50,break=50" -> CannotConvert("50", "thresholds.low", "'low' (50) must be greater than 'break' (50)")
      )

      testValues.foreach { case (config, expected) =>
        it(s"should load $config to expected result") {
          val result = ConfigSource.string(config).load[Thresholds]

          result match {
            case Right(value) => value shouldBe expected
            case Left(ConfigReaderFailures(ConvertFailure(reason, _, _), _*)) =>
              reason shouldBe expected
            case Left(other) => fail(s"unexpected value $other")
          }
        }
      }
    }

    // Scala is not parsed anymore in Stryker4jvm

//    describe("ScalaDialect") {
//      val validVersions = Map(
//        "scala212" -> Scala212,
//        "scala2.12" -> Scala212,
//        "2.12" -> Scala212,
//        "212" -> Scala212,
//        "scala212source3" -> Scala212Source3,
//        "scala213" -> Scala213,
//        "scala2.13" -> Scala213,
//        "2.13" -> Scala213,
//        "213" -> Scala213,
//        "2" -> Scala213,
//        "scala213source3" -> Scala213Source3,
//        "source3" -> Scala213Source3,
//        "scala3future" -> Scala3Future,
//        "future" -> Scala3Future,
//        "scala30" -> Scala30,
//        "scala3.0" -> Scala30,
//        "3.0" -> Scala30,
//        "30" -> Scala30,
//        "dotty" -> Scala30,
//        "scala31" -> Scala31,
//        "scala3.1" -> Scala31,
//        "3.1" -> Scala31,
//        "31" -> Scala31,
//        "scala32" -> Scala32,
//        "scala3.2" -> Scala32,
//        "3.2" -> Scala32,
//        "32" -> Scala32,
//        "scala3" -> Scala3,
//        "scala3.0" -> Scala3,
//        "3.0" -> Scala3,
//        "3" -> Scala3
//      )
//
//      validVersions.foreach { case (input, expected) =>
//        it(s"should parse $input to $expected") {
//          ExampleConfigs.scalaDialect(input).at("stryker4jvm").load[Config] match {
//            case Right(value) => {
//              value.mutatorConfigs.contains("scala") shouldBe true
//              value.mutatorConfigs("scala").getDialect shouldBe expected
//            }
//            case Left(value) => fail(s"Expected valid parsing, got $value")
//          }
//
//        }
//      }
//
//      it("should not parse invalid scala-dialects") {
//        expectConfigFailure(
//          ExampleConfigs.scalaDialect("foobar"),
//          CannotConvert(
//            "foobar",
//            "scala-dialect",
//            s"Unsupported dialect. Leaving this configuration empty defaults to scala213source3 which might also work for you. Valid scalaDialects are: ${
//              validVersions.keys
//                .map("'" + _ + "'")
//                .mkString(", ")
//            }"
//          )
//        )
//      }
//
//      val deprecatedVersions = List("scala211", "scala2.11", "2.11", "211")
//
//      deprecatedVersions.foreach { version =>
//        it(s"should error deprecated scala-dialect $version") {
//          expectConfigFailure(
//            ExampleConfigs.scalaDialect(version),
//            CannotConvert(
//              version,
//              "scala-dialect",
//              s"Deprecated dialect. Leaving this configuration empty defaults to scala213source3 which might also work for you. Valid scalaDialects are: ${
//                validVersions.keys
//                  .map("'" + _ + "'")
//                  .mkString(", ")
//              }"
//            )
//          )
//        }
//      }
//    }

    def expectConfigFailure(config: ConfigObjectSource, failure: FailureReason)(implicit pos: Position) =
      config.at("stryker4s").load[Config] match {
        case Left(ConfigReaderFailures(ConvertFailure(reason, _, _), _*)) =>
          reason.description shouldBe failure.description
          reason shouldBe failure
        case value => fail(s"Expected parsing failure but got $value")
      }
  }
}
