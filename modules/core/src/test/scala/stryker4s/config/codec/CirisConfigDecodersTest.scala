package stryker4s.config.codec

import cats.syntax.all.*
import ciris.{ConfigDecoder, ConfigKey}
import stryker4s.config.*
import stryker4s.testkit.Stryker4sSuite
import sttp.client4.UriContext

import scala.concurrent.duration.*
import scala.meta.dialects.*

class CirisConfigDecodersTest extends Stryker4sSuite with CirisConfigDecoders {
  val defaultConf = Config.default
  val default = defaultConf.thresholds
  val thresholdsKey = ConfigKey("ThresholdsKey")
  val thresholdsKeyDesc = thresholdsKey.description

  val validValues = Seq(
    "high=85, low=65, break=10" -> Thresholds(high = 85, low = 65, break = 10),
    "high=30, low=30" -> Thresholds(high = 30, low = 30, break = 0),
    "low=30, break=29" -> Thresholds(high = 60, low = 30, break = 29),
    "high=100" -> Thresholds(high = 100, low = 60, break = 0)
  )

  validValues.foreach { case (config, expected) =>
    test(s"Thresholds should load $config to expected result") {
      val result = validateThresholds.decode(thresholdsKey.some, expected).value

      assertEquals(result, expected)
    }
  }

  val invalidValues = Seq(
    default.copy(high = -1) -> Seq(s"$thresholdsKeyDesc with value -1 cannot be converted to percentage"),
    default.copy(low = -1) -> Seq(s"$thresholdsKeyDesc with value -1 cannot be converted to percentage"),
    default.copy(break = -1) -> Seq(s"$thresholdsKeyDesc with value -1 cannot be converted to percentage"),
    default.copy(high = 101) -> Seq(s"$thresholdsKeyDesc with value 101 cannot be converted to percentage"),
    default.copy(low = 101) -> Seq(s"$thresholdsKeyDesc with value 101 cannot be converted to percentage"),
    default.copy(break = 101) -> Seq(s"$thresholdsKeyDesc with value 101 cannot be converted to percentage"),
    default.copy(high = 50, low = 51) -> Seq(
      s"$thresholdsKeyDesc with value 50 cannot be converted to thresholds.high",
      "'high' (50) must be greater than or equal to 'low' (51)"
    ),
    default.copy(low = 50, break = 51) -> Seq(
      s"$thresholdsKeyDesc with value 50 cannot be converted to thresholds.low",
      "'low' (50) must be greater than 'break' (51)"
    ),
    default.copy(low = 50, break = 50) -> Seq(
      s"$thresholdsKeyDesc with value 50 cannot be converted to thresholds.low",
      "'low' (50) must be greater than 'break' (50)"
    )
  )

  invalidValues.foreach { case (config, expected) =>
    test(s"Thresholds should not load $config") {
      val result = validateThresholds.decode(thresholdsKey.some, config).leftValue.messages

      assertEquals(result.toList, expected)
    }
  }

  val dialectKey = ConfigKey("DialectKey")
  val dialectKeyDesc = dialectKey.description

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
    "scala34" -> Scala34,
    "scala3.4" -> Scala34,
    "3.4" -> Scala34,
    "34" -> Scala34,
    "scala35" -> Scala35,
    "scala3.5" -> Scala35,
    "3.5" -> Scala35,
    "35" -> Scala35,
    "scala36" -> Scala36,
    "scala3.6" -> Scala36,
    "3.6" -> Scala36,
    "36" -> Scala36,
    "scala37" -> Scala37,
    "scala3.7" -> Scala37,
    "3.7" -> Scala37,
    "37" -> Scala37,
    "scala38" -> Scala38,
    "scala3.8" -> Scala38,
    "3.8" -> Scala38,
    "38" -> Scala38,
    "scala39" -> Scala39,
    "scala3.9" -> Scala39,
    "3.9" -> Scala39,
    "39" -> Scala39,
    "scala3" -> Scala3,
    "3" -> Scala3
  )

  validVersions.foreach { case (input, expected) =>
    test(s"ScalaDialect should parse $input to $expected") {
      val result = dialectReader.decode(dialectKey.some, input).value
      assertEquals(result, expected)
    }
  }

  test("ScalaDialect should not parse invalid scala-dialects") {
    val result = dialectReader.decode(dialectKey.some, "foobar").leftValue.messages

    assertEquals(
      result.toList,
      List(
        s"$dialectKeyDesc with value foobar cannot be converted to scala-dialect",
        s"Unsupported dialect. Leaving this configuration empty defaults to scala213source3 which might also work for you. Valid scalaDialects are: ${validVersions.keys
            .map("'" + _ + "'")
            .mkString(", ")}"
      )
    )
  }

  val deprecatedVersions = List("scala211", "scala2.11", "2.11", "211")

  deprecatedVersions.foreach { version =>
    test(s"ScalaDialect should error deprecated scala-dialect $version") {
      val result = dialectReader.decode(dialectKey.some, version).leftValue.messages
      assertEquals(
        result.toList,
        List(
          s"$dialectKeyDesc with value $version cannot be converted to scala-dialect",
          s"Deprecated dialect. Leaving this configuration empty defaults to scala213source3 which might also work for you. Valid scalaDialects are: ${validVersions.keys
              .map("'" + _ + "'")
              .mkString(", ")}"
        )
      )
    }
  }

  test("ReporterType should decode valid reporters") {
    val validReporters = Seq("Console", "Html", "Json", "dashboard")
    val decoder = implicitly[ConfigDecoder[Seq[String], Seq[ReporterType]]]

    val result = decoder.decode(none, validReporters).value
    assertSameElements(result, Seq(Console, Html, Json, Dashboard))
  }

  test("ReporterType should not decode invalid reporters") {
    assertEquals(
      reporterDecoder.decode(none, "foobar").leftValue.messages.loneElement,
      "Unable to convert value foobar to reporter"
    )
  }

  test("DashboardReportType should decode valid dashboard report types") {

    assertEquals(dashboardReportTypeDecoder.decode(none, "Full").value, Full)
    assertEquals(dashboardReportTypeDecoder.decode(none, "MutationScoreOnly").value, MutationScoreOnly)
    assertEquals(dashboardReportTypeDecoder.decode(none, "mutation-score-only").value, MutationScoreOnly)
    assertEquals(dashboardReportTypeDecoder.decode(none, "ScoreOnly").value, MutationScoreOnly)
    assertEquals(dashboardReportTypeDecoder.decode(none, "score-only").value, MutationScoreOnly)
  }

  test("DashboardReportType should not decode invalid dashboard report types") {
    assertEquals(
      dashboardReportTypeDecoder.decode(none, "foobar").leftValue.messages.loneElement,
      "Unable to convert value foobar to dashboard.reportType"
    )
  }

  val uriKey = ConfigKey("UriKey")
  val uriKeyDesc = uriKey.description

  test("Uri should decode valid uris") {
    assertEquals(uriDecoder.decode(uriKey.some, "https://fakeurl.com").value, uri"https://fakeurl.com")
  }

  test("Uri should not decode invalid uris") {
    val result = uriDecoder.decode(uriKey.some, "").leftValue.messages

    assertEquals(
      result.toList,
      List(s"$uriKeyDesc with value  cannot be converted to uri", "empty string is not valid uri")
    )
  }

  test("Duration should decode valid duration values") {
    assertEquals(durationDecoder.decode(none, "5s").value, 5.seconds)
  }

  test("Duration should fallback to milliseconds when no unit is provided") {
    assertEquals(durationDecoder.decode(none, "5000").value, 5.seconds)
  }
}
