package stryker4s.report

import cats.syntax.all.*
import fs2.io.file.Path
import fs2.text
import mutationtesting.{Metrics, MutationTestResult, Thresholds}
import stryker4s.config.Config
import stryker4s.testkit.{LogMatchers, Stryker4sIOSuite}
import stryker4s.testutil.stubs.{DesktopIOStub, FileIOStub}

import scala.concurrent.duration.*

class HtmlReporterTest extends Stryker4sIOSuite with LogMatchers {

  implicit val config: Config = Config.default

  private val elementsLocation = "/elements/mutation-test-elements.js"

  describe("reportAsJsonStr") {
    test("should return a JSON string representation of the report") {
      val sut = new HtmlReporter(FileIOStub(), DesktopIOStub())
      val report = MutationTestResult(thresholds = Thresholds(100, 0), files = Map.empty)

      val result = sut.reportAsJsonStr(report)

      assertNoDiff(
        result,
        """      app.report = {"$schema":"https://git.io/mutation-testing-schema","schemaVersion":"2","thresholds":{"high":100,"low":0},"files":{}};"""
      )
    }

    test("escapes HTML inside the JSON") {
      val sut = new HtmlReporter(FileIOStub(), DesktopIOStub())
      val report = MutationTestResult(
        thresholds = Thresholds(100, 0),
        files = Map(
          "Example.scala" -> mutationtesting.FileResult(
            source = "<script>alert('boo')</script>",
            mutants = Seq.empty
          )
        )
      )

      val result = sut.reportAsJsonStr(report)

      assert(result.contains("<\"+\"script>alert('boo')<\"+\"/script>"))
      assert(!result.contains("<script>alert('boo')</script>"))
    }
  }

  describe("createHtmlReportStream") {
    test("should create a stream containing the HTML report") {
      val sut = new HtmlReporter(FileIOStub(), DesktopIOStub())
      val report = MutationTestResult(thresholds = Thresholds(100, 0), files = Map.empty)

      val resultStream = sut.createHtmlReportStream(report)

      resultStream
        .through(text.utf8.decode)
        .compile
        .string
        .asserting { resultString =>
          assert(resultString.startsWith("<!DOCTYPE html>"))
          assert(resultString.endsWith("</html>\n"))
          assert(resultString.contains(""""files":{}"""))
          assert(resultString.contains("""<mutation-test-report-app title-postfix="Stryker4s report">"""))
        }
    }
  }

  describe("onRunFinished") {
    val fileLocation = Path("target") / "stryker4s-report"

    test("should write the report files to the report directory") {
      val fileIOStub = FileIOStub()
      val desktopIOStub = DesktopIOStub()
      val sut = new HtmlReporter(fileIOStub, desktopIOStub)
      val report = MutationTestResult(thresholds = Thresholds(100, 0), files = Map.empty)
      val metrics = Metrics.calculateMetrics(report)

      sut
        .onRunFinished(FinishedRunEvent(report, metrics, 0.seconds, fileLocation)) >>
        (fileIOStub.resourceAsStreamCalls, fileIOStub.createAndWriteCalls).tupled.asserting {
          case (resourceCalls, createAndWriteCalls) =>
            createAndWriteCalls
              .map(_._1.toString)
              .foreach(fileName => assert(fileName.contains(fileLocation.toString), fileName))
            assertEquals(createAndWriteCalls.loneElement._1.fileName.toString, "index.html")
            assertEquals(resourceCalls.loneElement, elementsLocation)
        }
    }

    test("should write the mutation-test-elements.js file to the report directory") {
      val fileIOStub = FileIOStub()
      val desktopIOStub = DesktopIOStub()
      val sut = new HtmlReporter(fileIOStub, desktopIOStub)
      val report = MutationTestResult(thresholds = Thresholds(100, 0), files = Map.empty)
      val metrics = Metrics.calculateMetrics(report)

      sut
        .onRunFinished(FinishedRunEvent(report, metrics, 10.seconds, fileLocation)) >>
        fileIOStub.resourceAsStreamCalls.asserting { calls =>
          assertEquals(calls.loneElement, elementsLocation)
        }
    }

    test("should info log a message") {
      val fileIOStub = FileIOStub()
      val desktopIOStub = DesktopIOStub()
      val sut = new HtmlReporter(fileIOStub, desktopIOStub)
      val report = MutationTestResult(thresholds = Thresholds(100, 0), files = Map.empty)
      val metrics = Metrics.calculateMetrics(report)

      sut
        .onRunFinished(FinishedRunEvent(report, metrics, 10.seconds, fileLocation))
        .asserting { _ =>
          assertLoggedInfo(s"Written HTML report to ${(fileLocation / "index.html").toString}")
        }
    }

    test("should open the report when openReport is true") {
      implicit val config: Config = Config.default.copy(openReport = true)
      val fileIOStub = FileIOStub()
      val desktopIOStub = DesktopIOStub()

      val sut = new HtmlReporter(fileIOStub, desktopIOStub)
      val report = MutationTestResult(thresholds = Thresholds(100, 0), files = Map.empty)
      val metrics = Metrics.calculateMetrics(report)

      val expectedFileLocation = fileLocation / "index.html"

      sut
        .onRunFinished(FinishedRunEvent(report, metrics, 10.seconds, fileLocation)) >>
        desktopIOStub.openCalls.asserting { calls =>
          assertEquals(calls.loneElement, expectedFileLocation)
        }
    }

    test("should not open the report when openReport is false") {
      implicit val config: Config = Config.default.copy(openReport = false)
      val fileIOStub = FileIOStub()
      val desktopIOStub = DesktopIOStub()

      val sut = new HtmlReporter(fileIOStub, desktopIOStub)
      val report = MutationTestResult(thresholds = Thresholds(100, 0), files = Map.empty)
      val metrics = Metrics.calculateMetrics(report)

      sut
        .onRunFinished(FinishedRunEvent(report, metrics, 10.seconds, fileLocation)) >>
        desktopIOStub.openCalls.assertEquals(Seq.empty)
    }

    test("logs when opening the report fails") {
      implicit val config: Config = Config.default.copy(openReport = true)
      val fileIOStub = FileIOStub()
      val desktopIOStub = DesktopIOStub.throws()

      val sut = new HtmlReporter(fileIOStub, desktopIOStub)
      val report = MutationTestResult(thresholds = Thresholds(100, 0), files = Map.empty)
      val metrics = Metrics.calculateMetrics(report)

      sut
        .onRunFinished(FinishedRunEvent(report, metrics, 10.seconds, fileLocation)) >>
        desktopIOStub.openCalls.asserting { _ =>
          assertLoggedError("Error opening report in browser")
        }
    }
  }
}
