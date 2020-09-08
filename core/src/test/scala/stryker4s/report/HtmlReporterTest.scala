package stryker4s.report
import java.nio.file.Path

import scala.concurrent.duration._

import better.files.File
import cats.effect.IO
import mutationtesting.{Metrics, MutationTestReport, Thresholds}
import org.mockito.captor.ArgCaptor
import stryker4s.files.{DiskFileIO, FileIO}
import stryker4s.scalatest.LogMatchers
import stryker4s.testutil.{MockitoSuite, Stryker4sSuite}

class HtmlReporterTest extends Stryker4sSuite with MockitoSuite with LogMatchers {

  private val elementsLocation = "/mutation-testing-elements/mutation-test-elements.js"

  private val expectedHtml =
    """<!DOCTYPE html>
      |<html lang="en">
      |<head>
      |  <meta charset="UTF-8">
      |  <meta name="viewport" content="width=device-width, initial-scale=1.0">
      |  <script src="mutation-test-elements.js"></script>
      |</head>
      |<body>
      |  <mutation-test-report-app title-postfix="Stryker4s report">
      |    Your browser doesn't support <a href="https://caniuse.com/#search=custom%20elements">custom elements</a>.
      |    Please use a latest version of an evergreen browser (Firefox, Chrome, Safari, Opera, etc).
      |  </mutation-test-report-app>
      |  <script src="report.js"></script>
      |</body>
      |</html>""".stripMargin

  describe("indexHtml") {
    it("should contain title") {
      val mockFileIO = mock[FileIO]
      when(mockFileIO.createAndWrite(any[Path], any[String])).thenReturn(IO.unit)
      val sut = new HtmlReporter(mockFileIO)
      val testFile = File("foo.bar").path

      sut
        .writeIndexHtmlTo(testFile)
        .unsafeRunSync()

      verify(mockFileIO).createAndWrite(testFile, expectedHtml)
    }
  }

  describe("reportJs") {
    it("should contain the report") {
      val mockFileIO = mock[FileIO]
      when(mockFileIO.createAndWrite(any[Path], any[String])).thenReturn(IO.unit)
      val sut = new HtmlReporter(mockFileIO)
      val testFile = File("foo.bar").path
      val runResults = MutationTestReport(thresholds = Thresholds(100, 0), files = Map.empty)

      sut
        .writeReportJsTo(testFile, runResults)
        .unsafeRunSync()

      val expectedJs =
        """document.querySelector('mutation-test-report-app').report = {"$schema":"https://git.io/mutation-testing-report-schema","schemaVersion":"1","thresholds":{"high":100,"low":0},"files":{}}"""
      verify(mockFileIO).createAndWrite(testFile, expectedJs)
    }
  }

  describe("mutation-test-elements") {
    it("should write the resource") {
      val fileIO = new DiskFileIO()

      File.usingTemporaryDirectory() { tmpDir =>
        val tempFile = tmpDir / "mutation-test-elements.js"
        val sut = new HtmlReporter(fileIO)

        sut.writeMutationTestElementsJsTo(tempFile.path).attempt.unsafeRunSync()

        val atLeastSize: Long = 100 * 1024L // 100KB
        tempFile.size should be > atLeastSize
        tempFile.lineIterator
          .next() shouldEqual "/*! For license information please see mutation-test-elements.js.LICENSE.txt */"
      }
    }
  }

  describe("reportRunFinished") {
    it("should write the report files to the report directory") {
      val mockFileIO = mock[FileIO]
      when(mockFileIO.createAndWrite(any[Path], any[String])).thenReturn(IO.unit)
      when(mockFileIO.createAndWriteFromResource(any[Path], any[String])).thenReturn(IO.unit)
      val sut = new HtmlReporter(mockFileIO)
      val report = MutationTestReport(thresholds = Thresholds(100, 0), files = Map.empty)
      val metrics = Metrics.calculateMetrics(report)

      sut
        .reportRunFinished(FinishedRunReport(report, metrics, 0.seconds, File("target/stryker4s-report/")))
        .unsafeRunSync()

      val writtenFilesCaptor = ArgCaptor[Path]
      verify(mockFileIO, times(2)).createAndWrite(writtenFilesCaptor, any[String])
      verify(mockFileIO).createAndWriteFromResource(any[Path], eqTo(elementsLocation))
      val paths = writtenFilesCaptor.values.map(_.toString())
      all(paths) should include("/target/stryker4s-report/")
      writtenFilesCaptor.values.map(_.getFileName().toString()) should contain.only("index.html", "report.js")
    }

    it("should write the mutation-test-elements.js file to the report directory") {
      val mockFileIO = mock[FileIO]
      when(mockFileIO.createAndWrite(any[Path], any[String])).thenReturn(IO.unit)
      when(mockFileIO.createAndWriteFromResource(any[Path], any[String])).thenReturn(IO.unit)
      val sut = new HtmlReporter(mockFileIO)
      val report = MutationTestReport(thresholds = Thresholds(100, 0), files = Map.empty)
      val metrics = Metrics.calculateMetrics(report)

      sut
        .reportRunFinished(FinishedRunReport(report, metrics, 10.seconds, File("target/stryker4s-report/")))
        .unsafeRunSync()

      val elementsCaptor = ArgCaptor[Path]
      verify(mockFileIO, times(2)).createAndWrite(any[Path], any[String])
      verify(mockFileIO).createAndWriteFromResource(elementsCaptor, eqTo(elementsLocation))

      elementsCaptor.value.toString should endWith("/target/stryker4s-report/mutation-test-elements.js")
    }

    it("should info log a message") {
      val mockFileIO = mock[FileIO]
      when(mockFileIO.createAndWrite(any[Path], any[String])).thenReturn(IO.unit)
      when(mockFileIO.createAndWriteFromResource(any[Path], any[String])).thenReturn(IO.unit)
      val sut = new HtmlReporter(mockFileIO)
      val report = MutationTestReport(thresholds = Thresholds(100, 0), files = Map.empty)
      val metrics = Metrics.calculateMetrics(report)
      val reportFile = File("target/stryker4s-report/")
      sut
        .reportRunFinished(FinishedRunReport(report, metrics, 10.seconds, reportFile))
        .unsafeRunSync()

      val captor = ArgCaptor[Path]
      verify(mockFileIO).createAndWrite(captor.capture, eqTo(expectedHtml))
      verify(mockFileIO, times(2)).createAndWrite(any[Path], any[String])
      verify(mockFileIO).createAndWriteFromResource(any[Path], any[String])
      s"Written HTML report to ${reportFile.toString()}/index.html" shouldBe loggedAsInfo
    }
  }
}
