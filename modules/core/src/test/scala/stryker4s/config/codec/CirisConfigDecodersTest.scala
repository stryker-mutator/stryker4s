package stryker4s.config.codec

import cats.syntax.all.*
import ciris.{ConfigDecoder, ConfigKey}
import stryker4s.config.*
import stryker4s.testkit.Stryker4sSuite
import sttp.client3.*

import scala.concurrent.duration.*
import scala.meta.dialects.*

class CirisConfigDecodersTest extends Stryker4sSuite with CirisConfigDecoders {
  val defaultConf = Config.default
  describe("Thresholds") {
    val default = defaultConf.thresholds
    val key = ConfigKey("ThresholdsKey")
    val keyDesc = key.description

    val validValues = Seq(
      "high=85, low=65, break=10" -> Thresholds(high = 85, low = 65, break = 10),
      "high=30, low=30" -> Thresholds(high = 30, low = 30, break = 0),
      "low=30, break=29" -> Thresholds(high = 60, low = 30, break = 29),
      "high=100" -> Thresholds(high = 100, low = 60, break = 0)
    )

    validValues.foreach { case (config, expected) =>
      test(s"should load $config to expected result") {
        val result = validateThresholds.decode(key.some, expected).value

        assertEquals(result, expected)
      }
    }

    val invalidValues = Seq(
      default.copy(high = -1) -> Seq(s"$keyDesc with value -1 cannot be converted to percentage"),
      default.copy(low = -1) -> Seq(s"$keyDesc with value -1 cannot be converted to percentage"),
      default.copy(break = -1) -> Seq(s"$keyDesc with value -1 cannot be converted to percentage"),
      default.copy(high = 101) -> Seq(s"$keyDesc with value 101 cannot be converted to percentage"),
      default.copy(low = 101) -> Seq(s"$keyDesc with value 101 cannot be converted to percentage"),
      default.copy(break = 101) -> Seq(s"$keyDesc with value 101 cannot be converted to percentage"),
      default.copy(high = 50, low = 51) -> Seq(
        s"$keyDesc with value 50 cannot be converted to thresholds.high",
        "'high' (50) must be greater than or equal to 'low' (51)"
      ),
      default.copy(low = 50, break = 51) -> Seq(
        s"$keyDesc with value 50 cannot be converted to thresholds.low",
        "'low' (50) must be greater than 'break' (51)"
      ),
      default.copy(low = 50, break = 50) -> Seq(
        s"$keyDesc with value 50 cannot be converted to thresholds.low",
        "'low' (50) must be greater than 'break' (50)"
      )
    )

    invalidValues.foreach { case (config, expected) =>
      test(s"should not load $config") {
        val result = validateThresholds.decode(key.some, config).leftValue.messages

        assertEquals(result.toList, expected)
      }
    }
  }

  describe("ScalaDialect") {
    val key = ConfigKey("DialectKey")
    val keyDesc = key.description

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
        val result = dialectReader.decode(key.some, input).value
        assertEquals(result, expected)
      }
    }

    test("should not parse invalid scala-dialects") {
      val result = dialectReader.decode(key.some, "foobar").leftValue.messages

      assertEquals(
        result.toList,
        List(
          s"$keyDesc with value foobar cannot be converted to scala-dialect",
          s"Unsupported dialect. Leaving this configuration empty defaults to scala213source3 which might also work for you. Valid scalaDialects are: ${validVersions.keys
              .map("'" + _ + "'")
              .mkString(", ")}"
        )
      )
    }

    val deprecatedVersions = List("scala211", "scala2.11", "2.11", "211")

    deprecatedVersions.foreach { version =>
      test(s"should error deprecated scala-dialect $version") {
        val result = dialectReader.decode(key.some, version).leftValue.messages
        assertEquals(
          result.toList,
          List(
            s"$keyDesc with value $version cannot be converted to scala-dialect",
            s"Deprecated dialect. Leaving this configuration empty defaults to scala213source3 which might also work for you. Valid scalaDialects are: ${validVersions.keys
                .map("'" + _ + "'")
                .mkString(", ")}"
          )
        )
      }
    }
  }

  describe("ReporterType") {
    test("should decode valid reporters") {
      val validReporters = Seq("Console", "Html", "Json", "dashboard")
      val decoder = implicitly[ConfigDecoder[Seq[String], Seq[ReporterType]]]

      val result = decoder.decode(none, validReporters).value
      assertSameElements(result, Seq(Console, Html, Json, Dashboard))
    }

    test("should not decode invalid reporters") {
      assertEquals(
        reporterDecoder.decode(none, "foobar").leftValue.messages.loneElement,
        "Unable to convert value foobar to reporter"
      )
    }
  }

  describe("DashboardReportType") {
    test("should decode valid dashboard report types") {

      assertEquals(dashboardReportTypeDecoder.decode(none, "Full").value, Full)
      assertEquals(dashboardReportTypeDecoder.decode(none, "MutationScoreOnly").value, MutationScoreOnly)
      assertEquals(dashboardReportTypeDecoder.decode(none, "mutation-score-only").value, MutationScoreOnly)
      assertEquals(dashboardReportTypeDecoder.decode(none, "ScoreOnly").value, MutationScoreOnly)
      assertEquals(dashboardReportTypeDecoder.decode(none, "score-only").value, MutationScoreOnly)
    }

    test("should not decode invalid dashboard report types") {
      assertEquals(
        dashboardReportTypeDecoder.decode(none, "foobar").leftValue.messages.loneElement,
        "Unable to convert value foobar to dashboard.reportType"
      )
    }
  }

  describe("Uri") {
    val key = ConfigKey("UriKey")
    val keyDesc = key.description

    test("should decode valid uris") {
      assertEquals(uriDecoder.decode(key.some, "https://fakeurl.com").value, uri"https://fakeurl.com")
    }

    test("should not decode invalid uris") {
      val result = uriDecoder.decode(key.some, "").leftValue.messages

      assertEquals(
        result.toList,
        List(s"$keyDesc with value  cannot be converted to uri", "empty string is not valid uri")
      )
    }
  }

  describe("Duration") {
    test("should decode valid duration values") {
      assertEquals(durationDecoder.decode(none, "5s").value, 5.seconds)
    }

    test("should fallback to milliseconds when no unit is provided") {
      assertEquals(durationDecoder.decode(none, "5000").value, 5.seconds)
    }
  }
}
