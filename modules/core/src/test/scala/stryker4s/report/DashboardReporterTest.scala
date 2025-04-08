package stryker4s.report

import cats.data.NonEmptyChain
import cats.effect.IO
import cats.syntax.all.*
import fansi.Bold
import fansi.Color.Red
import fs2.io.file.Path
import io.circe.Json
import io.circe.syntax.*
import mutationtesting.*
import mutationtesting.circe.*
import stryker4s.config.codec.CirceConfigEncoder
import stryker4s.config.{Full, MutationScoreOnly}
import stryker4s.report.model.DashboardConfig
import stryker4s.testkit.{LogMatchers, Stryker4sIOSuite}
import stryker4s.testutil.stubs.DashboardConfigProviderStub
import sttp.client4.*
import sttp.model.{Header, MediaType, Method}

import scala.concurrent.duration.*

class DashboardReporterTest extends Stryker4sIOSuite with LogMatchers with CirceConfigEncoder {
  describe("buildRequest") {
    test("should compose the request") {
      implicit val backend = backendStub
      val dashConfigProvider = DashboardConfigProviderStub(baseDashConfig)
      val sut = new DashboardReporter(dashConfigProvider)
      val dashConfig = baseDashConfig
      val FinishedRunEvent(report, metrics, _, _) = baseResults

      val request = sut.buildRequest(dashConfig, report, metrics)
      assertEquals(request.uri, uri"https://baseurl.com/api/reports/project/foo/version/bar")
      val jsonBody = report.asJson.noSpaces

      assertEquals(request.body, StringBody(jsonBody, "utf-8", MediaType.ApplicationJson))
      assertEquals(request.method, Method.PUT)
      assertSameElements(
        request.headers,
        List(
          Header.acceptEncoding("gzip, deflate"),
          new Header("X-Api-Key", "apiKeyHere"),
          Header.contentType(MediaType.ApplicationJson),
          Header.contentLength(jsonBody.length().toLong)
        )
      )
    }

    test("should make a score-only request when score-only is configured") {
      implicit val backend = backendStub
      val dashConfigProvider = DashboardConfigProviderStub(baseDashConfig)
      val sut = new DashboardReporter(dashConfigProvider)
      val dashConfig = baseDashConfig.copy(reportType = MutationScoreOnly)
      val FinishedRunEvent(report, metrics, _, _) = baseResults

      val request = sut.buildRequest(dashConfig, report, metrics)
      assertEquals(request.uri, uri"https://baseurl.com/api/reports/project/foo/version/bar")
      val jsonBody = """{"mutationScore":100.0}"""
      assertEquals(request.body, StringBody(jsonBody, "utf-8", MediaType.ApplicationJson))
    }

    test("should add the module if it is present") {
      implicit val backend = backendStub
      val dashConfigProvider = DashboardConfigProviderStub(baseDashConfig)
      val sut = new DashboardReporter(dashConfigProvider)
      val dashConfig = baseDashConfig.copy(module = "myModule".some)
      val FinishedRunEvent(report, metrics, _, _) = baseResults

      val request = sut.buildRequest(dashConfig, report, metrics)

      assertEquals(request.uri, uri"https://baseurl.com/api/reports/project/foo/version/bar?module=myModule")
    }
  }

  describe("onRunFinished") {
    test("should send the request") {
      implicit val backend = backendStub.map(
        _.whenAnyRequest
          .thenRespondAdjust(Json.obj(("href", Json.fromString("https://hrefHere.com"))).noSpaces)
      )
      val dashConfigProvider = DashboardConfigProviderStub(baseDashConfig)
      val sut = new DashboardReporter(dashConfigProvider)
      val runReport = baseResults

      sut
        .onRunFinished(runReport)
        .asserting { _ =>
          assertLoggedInfo("Sent report to dashboard. Available at https://hrefHere.com")
        }
    }

    test("log when not being able to resolve dashboard config") {
      implicit val backend = backendStub
      val dashConfigProvider = DashboardConfigProviderStub.invalid(NonEmptyChain("fooConfigKey", "barConfigKey"))
      val sut = new DashboardReporter(dashConfigProvider)
      val runReport = baseResults

      sut
        .onRunFinished(runReport)
        .asserting { _ =>
          assertLoggedWarn(
            s"Could not resolve dashboard configuration key(s) '${Bold.On("fooConfigKey")}', '${Bold.On("barConfigKey")}'. Not sending report."
          )
        }
    }

    test("should log when a response can't be parsed to a href") {
      implicit val backend = backendStub.map(_.whenAnyRequest.thenRespondAdjust("some other response"))
      val dashConfigProvider = DashboardConfigProviderStub(baseDashConfig)
      val sut = new DashboardReporter(dashConfigProvider)
      val runReport = baseResults

      sut
        .onRunFinished(runReport)
        .asserting { _ =>
          assertLoggedWarn(
            "Dashboard report was sent successfully, but could not decode the response: 'some other response'. Error:"
          )
        }
    }

    test("should log when a 401 is returned by the API") {
      implicit val backend = backendStub.map(
        _.whenAnyRequest
          .thenRespondUnauthorized()
      )
      val dashConfigProvider = DashboardConfigProviderStub(baseDashConfig)
      val sut = new DashboardReporter(dashConfigProvider)
      val runReport = baseResults

      sut
        .onRunFinished(runReport)
        .asserting { _ =>
          assertLoggedError(
            s"Error HTTP PUT 'Unauthorized'. Status code ${Red("401 Unauthorized")}. Did you provide the correct api key in the '${Bold
                .On("STRYKER_DASHBOARD_API_KEY")}' environment variable?"
          )
        }
    }

    test("should log when a error code is returned by the API") {
      implicit val backend =
        backendStub.map(
          _.whenAnyRequest.thenRespondServerError()
        )
      val dashConfigProvider = DashboardConfigProviderStub(baseDashConfig)
      val sut = new DashboardReporter(dashConfigProvider)
      val runReport = baseResults

      sut
        .onRunFinished(runReport)
        .asserting { _ =>
          assertLoggedError(
            s"Failed to PUT report to dashboard. Response status code: ${Red("500")}. Response body: 'Internal Server Error'"
          )
        }
    }
  }

  def backendStub =
    sttp.client4.httpclient.fs2.HttpClientFs2Backend.stub[IO].pure[IO].toResource

  def baseResults = {
    val files =
      Map(
        "stryker4s.scala" ->
          FileResult(
            "package stryker4s",
            Seq(MutantResult("1", "-", "+", Location(Position(0, 0), Position(1, 0)), MutantStatus.Killed))
          )
      )
    val report = MutationTestResult(thresholds = mutationtesting.Thresholds(80, 60), files = files)
    val metrics = Metrics.calculateMetrics(report)
    FinishedRunEvent(report, metrics, 15.seconds, Path("target/stryker4s-report/"))
  }

  def baseDashConfig =
    DashboardConfig(
      apiKey = "apiKeyHere",
      reportType = Full,
      baseUrl = uri"https://baseurl.com",
      project = "project/foo",
      version = "version/bar",
      module = none
    )
}
