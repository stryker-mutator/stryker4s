package stryker4s.report

import cats.effect.{IO, Resource}
import fs2.io.file.Path
import mutationtesting._
import stryker4s.config.{Full, MutationScoreOnly}
import stryker4s.report.dashboard.DashboardConfigProvider
import stryker4s.report.model.{DashboardConfig, DashboardPutResult}
import stryker4s.scalatest.LogMatchers
import stryker4s.testutil.{MockitoIOSuite, Stryker4sIOSuite}
import sttp.client3._
import sttp.client3.testing.SttpBackendStub
import sttp.model.{Header, MediaType, Method, StatusCode}

import scala.concurrent.duration._

class DashboardReporterTest extends Stryker4sIOSuite with MockitoIOSuite with LogMatchers {
  describe("buildRequest") {
    it("should compose the request") {
      implicit val backend = backendStub
      val mockDashConfig = mock[DashboardConfigProvider]
      val sut = new DashboardReporter(mockDashConfig)
      val dashConfig = baseDashConfig
      val FinishedRunEvent(report, metrics, _, _) = baseResults

      val request = sut.buildRequest(dashConfig, report, metrics)
      request.uri shouldBe uri"https://baseurl.com/api/reports/project/foo/version/bar"
      val jsonBody = {
        import mutationtesting.circe._
        import io.circe.syntax._
        report.asJson.noSpaces
      }
      request.body shouldBe StringBody(jsonBody, "utf-8", MediaType.ApplicationJson)
      request.method shouldBe Method.PUT
      request.headers should (contain.allOf(
        new Header("X-Api-Key", "apiKeyHere"),
        new Header("Content-Type", "application/json")
      ))
    }

    it("should make a score-only request when score-only is configured") {
      implicit val backend = backendStub
      val mockDashConfig = mock[DashboardConfigProvider]
      val sut = new DashboardReporter(mockDashConfig)
      val dashConfig = baseDashConfig.copy(reportType = MutationScoreOnly)
      val FinishedRunEvent(report, metrics, _, _) = baseResults

      val request = sut.buildRequest(dashConfig, report, metrics)
      request.uri shouldBe uri"https://baseurl.com/api/reports/project/foo/version/bar"
      val jsonBody = """{"mutationScore":100.0}"""
      request.body shouldBe StringBody(jsonBody, "utf-8", MediaType.ApplicationJson)
    }

    it("should add the module if it is present") {
      implicit val backend = backendStub
      val mockDashConfig = mock[DashboardConfigProvider]
      val sut = new DashboardReporter(mockDashConfig)
      val dashConfig = baseDashConfig.copy(module = Some("myModule"))
      val FinishedRunEvent(report, metrics, _, _) = baseResults

      val request = sut.buildRequest(dashConfig, report, metrics)

      request.uri shouldBe uri"https://baseurl.com/api/reports/project/foo/version/bar?module=myModule"
    }
  }

  describe("onRunFinished") {
    it("should send the request") {
      implicit val backend = backendStub.map(
        _.whenAnyRequest
          .thenRespond(Right(DashboardPutResult("https://hrefHere.com")))
      )
      val mockDashConfig = mock[DashboardConfigProvider]
      when(mockDashConfig.resolveConfig()).thenReturn(Right(baseDashConfig))
      val sut = new DashboardReporter(mockDashConfig)
      val runReport = baseResults

      sut
        .onRunFinished(runReport)
        .asserting { _ =>
          "Sent report to dashboard. Available at https://hrefHere.com" shouldBe loggedAsInfo
        }
    }

    it("log when not being able to resolve dashboard config") {
      implicit val backend = backendStub
      val mockDashConfig = mock[DashboardConfigProvider]
      when(mockDashConfig.resolveConfig()).thenReturn(Left("fooConfigKey"))
      val sut = new DashboardReporter(mockDashConfig)
      val runReport = baseResults

      sut
        .onRunFinished(runReport)
        .asserting { _ =>
          "Could not resolve dashboard configuration key 'fooConfigKey', not sending report" shouldBe loggedAsWarning
        }
    }

    it("should log when a response can't be parsed to a href") {
      implicit val backend = backendStub.map(_.whenAnyRequest.thenRespond("some other response"))
      val mockDashConfig = mock[DashboardConfigProvider]
      when(mockDashConfig.resolveConfig()).thenReturn(Right(baseDashConfig))
      val sut = new DashboardReporter(mockDashConfig)
      val runReport = baseResults

      sut
        .onRunFinished(runReport)
        .asserting { _ =>
          "Dashboard report was sent successfully, but could not decode the response: 'some other response'. Error:" shouldBe loggedAsWarning
        }
    }

    it("should log when a 401 is returned by the API") {
      implicit val backend = backendStub.map(
        _.whenAnyRequest
          .thenRespond(Response(Left(HttpError("auth required", StatusCode.Unauthorized)), StatusCode.Unauthorized))
      )
      val mockDashConfig = mock[DashboardConfigProvider]
      when(mockDashConfig.resolveConfig()).thenReturn(Right(baseDashConfig))
      val sut = new DashboardReporter(mockDashConfig)
      val runReport = baseResults

      sut
        .onRunFinished(runReport)
        .asserting { _ =>
          "Error HTTP PUT 'auth required'. Status code 401 Unauthorized. Did you provide the correct api key in the 'STRYKER_DASHBOARD_API_KEY' environment variable?" shouldBe loggedAsError
        }
    }

    it("should log when a error code is returned by the API") {
      implicit val backend =
        backendStub.map(
          _.whenAnyRequest.thenRespond(
            Response(Left(HttpError("internal error", StatusCode.InternalServerError)), StatusCode.InternalServerError)
          )
        )
      val mockDashConfig = mock[DashboardConfigProvider]
      when(mockDashConfig.resolveConfig()).thenReturn(Right(baseDashConfig))
      val sut = new DashboardReporter(mockDashConfig)
      val runReport = baseResults

      sut
        .onRunFinished(runReport)
        .asserting { _ =>
          "Failed to PUT report to dashboard. Response status code: 500. Response body: 'internal error'" shouldBe loggedAsError
        }
    }
  }

  def backendStub =
    Resource.pure[IO, SttpBackendStub[IO, Any]](sttp.client3.httpclient.fs2.HttpClientFs2Backend.stub[IO])

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
      module = None
    )
}
