package stryker4s.report

import stryker4s.scalatest.LogMatchers
import stryker4s.testutil.{MockitoSuite, Stryker4sSuite}
import sttp.client.testing.SttpBackendStub
import stryker4s.report.dashboard.DashboardConfigProvider
import stryker4s.report.model.DashboardConfig
import stryker4s.config.Full
import mutationtesting.MutationTestReport
import mutationtesting.Metrics
import sttp.client._
import sttp.model.Header
import sttp.model.Method
import stryker4s.report.model.DashboardPutResult
import sttp.model.MediaType
import stryker4s.config.MutationScoreOnly
import sttp.model.StatusCode
import mutationtesting._

class DashboardReporterTest extends Stryker4sSuite with MockitoSuite with LogMatchers {
  describe("buildRequest") {
    it("should compose the request") {
      implicit val backend = backendStub
      val mockDashConfig = mock[DashboardConfigProvider]
      val sut = new DashboardReporter(mockDashConfig)
      val dashConfig = baseDashConfig
      val FinishedRunReport(report, metrics) = baseResults

      val request = sut.buildRequest(dashConfig, report, metrics)
      request.uri shouldBe uri"https://baseurl.com/api/reports/project/foo/version/bar"
      val jsonBody = {
        import mutationtesting.MutationReportEncoder._
        import io.circe.syntax._
        report.asJson.noSpaces
      }
      request.body shouldBe StringBody(jsonBody, "utf-8", Some(MediaType.ApplicationJson))
      request.method shouldBe Method.PUT
      request.headers should contain allOf (
        new Header("X-Api-Key", "apiKeyHere"),
        new Header("Content-Type", "application/json")
      )
    }

    it("should make a score-only request when score-only is configured") {
      implicit val backend = backendStub
      val mockDashConfig = mock[DashboardConfigProvider]
      val sut = new DashboardReporter(mockDashConfig)
      val dashConfig = baseDashConfig.copy(reportType = MutationScoreOnly)
      val FinishedRunReport(report, metrics) = baseResults

      val request = sut.buildRequest(dashConfig, report, metrics)
      request.uri shouldBe uri"https://baseurl.com/api/reports/project/foo/version/bar"
      val jsonBody = """{"mutationScore":100.0}"""
      request.body shouldBe StringBody(jsonBody, "utf-8", Some(MediaType.ApplicationJson))
    }

    it("should add the module if it is present") {
      implicit val backend = backendStub
      val mockDashConfig = mock[DashboardConfigProvider]
      val sut = new DashboardReporter(mockDashConfig)
      val dashConfig = baseDashConfig.copy(module = Some("myModule"))
      val FinishedRunReport(report, metrics) = baseResults

      val request = sut.buildRequest(dashConfig, report, metrics)

      request.uri shouldBe uri"https://baseurl.com/api/reports/project/foo/version/bar?module=myModule"
    }
  }

  describe("reportRunFinished") {
    it("should send the request") {
      implicit val backend = backendStub.whenAnyRequest
        .thenRespond(Right(DashboardPutResult("https://hrefHere.com")))
      val mockDashConfig = mock[DashboardConfigProvider]
      when(mockDashConfig.resolveConfig()).thenReturn(Right(baseDashConfig))
      val sut = new DashboardReporter(mockDashConfig)
      val runReport = baseResults

      sut.reportRunFinished(runReport)

      "Sent report to dashboard. Available at https://hrefHere.com" shouldBe loggedAsInfo
    }

    it("log when not being able to resolve dashboard config") {
      implicit val backend = backendStub
      val mockDashConfig = mock[DashboardConfigProvider]
      when(mockDashConfig.resolveConfig()).thenReturn(Left("fooConfigKey"))
      val sut = new DashboardReporter(mockDashConfig)
      val runReport = baseResults

      sut.reportRunFinished(runReport)

      "Could not resolve dashboard configuration key 'fooConfigKey', not sending report" shouldBe loggedAsWarning
    }

    it("should log when a response can't be parsed to a href") {
      implicit val backend = backendStub.whenAnyRequest.thenRespond("some other response")
      val mockDashConfig = mock[DashboardConfigProvider]
      when(mockDashConfig.resolveConfig()).thenReturn(Right(baseDashConfig))
      val sut = new DashboardReporter(mockDashConfig)
      val runReport = baseResults

      sut.reportRunFinished(runReport)

      "Dashboard report was sent successfully, but could not decode the response: 'some other response'. Error:" shouldBe loggedAsWarning
    }

    it("should log when a 401 is returned by the API") {
      implicit val backend = backendStub.whenAnyRequest
        .thenRespond(Response(Left(HttpError("auth required")), StatusCode.Unauthorized))
      val mockDashConfig = mock[DashboardConfigProvider]
      when(mockDashConfig.resolveConfig()).thenReturn(Right(baseDashConfig))
      val sut = new DashboardReporter(mockDashConfig)
      val runReport = baseResults

      sut.reportRunFinished(runReport)

      "Error HTTP PUT 'auth required'. Status code 401 Unauthorized. Did you provide the correct api key in the 'STRYKER_DASHBOARD_API_KEY' environment variable?" shouldBe loggedAsError
    }

    it("should log when a error code is returned by the API") {
      implicit val backend =
        backendStub.whenAnyRequest.thenRespond(
          Response(Left(HttpError("internal error")), StatusCode.InternalServerError)
        )
      val mockDashConfig = mock[DashboardConfigProvider]
      when(mockDashConfig.resolveConfig()).thenReturn(Right(baseDashConfig))
      val sut = new DashboardReporter(mockDashConfig)
      val runReport = baseResults

      sut.reportRunFinished(runReport)

      "Failed to PUT report to dashboard. Response status code: 500. Response body: 'internal error'" shouldBe loggedAsError
    }
  }

  def backendStub = SttpBackendStub.synchronous

  def baseResults = {
    val files =
      Map(
        "stryker4s.scala" ->
          MutationTestResult(
            "package stryker4s",
            Seq(MutantResult("1", "-", "+", Location(Position(0, 0), Position(1, 0)), MutantStatus.Killed))
          )
      )
    val report = MutationTestReport(thresholds = mutationtesting.Thresholds(80, 60), files = files)
    val metrics = Metrics.calculateMetrics(report)
    FinishedRunReport(report, metrics)
  }

  def baseDashConfig = DashboardConfig(
    apiKey = "apiKeyHere",
    reportType = Full,
    baseUrl = "https://baseurl.com",
    project = "project/foo",
    version = "version/bar",
    module = None
  )
}
