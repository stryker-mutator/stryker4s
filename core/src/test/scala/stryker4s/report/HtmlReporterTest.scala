package stryker4s.report
import better.files.File
import mutationtesting.{Metrics, MutationTestReport, Thresholds}
import org.mockito.captor.ArgCaptor
import stryker4s.config.Config
import stryker4s.files.{DiskFileIO, FileIO}
import stryker4s.scalatest.LogMatchers
import stryker4s.testutil.{MockitoSuite, Stryker4sSuite}
import java.nio.file.Path
import cats.effect.IO

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
      implicit val config: Config = Config.default
      val mockFileIO = mock[FileIO]
      when(mockFileIO.createAndWrite(any[Path], any[String])).thenReturn(IO.unit)
      val sut = new HtmlReporter(mockFileIO)
      val testFile = (config.baseDir / "foo.bar").path

      sut
        .writeIndexHtmlTo(testFile)
        .unsafeRunSync()

      verify(mockFileIO).createAndWrite(testFile, expectedHtml)
    }
  }

  describe("reportJs") {
    it("should contain the report") {
      implicit val config: Config = Config.default
      val mockFileIO = mock[FileIO]
      when(mockFileIO.createAndWrite(any[Path], any[String])).thenReturn(IO.unit)
      val sut = new HtmlReporter(mockFileIO)
      val testFile = (config.baseDir / "foo.bar").path
      val runResults = MutationTestReport(thresholds = Thresholds(100, 0), files = Map.empty)

      sut
        .writeReportJsTo(testFile, runResults)
        .unsafeRunSync()

      val expectedJs =
        """document.querySelector('mutation-test-report-app').report = {"$schema":"https://raw.githubusercontent.com/stryker-mutator/mutation-testing-elements/master/packages/mutation-testing-report-schema/src/mutation-testing-report-schema.json","schemaVersion":"1","thresholds":{"high":100,"low":0},"files":{}}"""
      verify(mockFileIO).createAndWrite(testFile, expectedJs)
    }
  }

  describe("mutation-test-elements") {
    it("should write the resource") {
      implicit val config: Config = Config.default
      val fileIO = new DiskFileIO()

      val tempFile = File.temp / "mutation-test-elements.js"
      val sut = new HtmlReporter(fileIO)

      sut.writeMutationTestElementsJsTo(tempFile.path).attempt.unsafeRunSync

      val atLeastSize: Long = 100 * 1024L // 100KB
      tempFile.size should be > atLeastSize
      tempFile.lineIterator
        .next() shouldEqual "/*! For license information please see mutation-test-elements.js.LICENSE.txt */"
    }
  }

  describe("reportRunFinished") {
    implicit val config: Config = Config.default
    val stryker4sReportFolderRegex = ".*target(/|\\\\)stryker4s-report-(\\d*)(/|\\\\)[a-z-]*\\.[a-z]*$"

    it("should write the report files to the report directory") {
      val mockFileIO = mock[FileIO]
      when(mockFileIO.createAndWrite(any[Path], any[String])).thenReturn(IO.unit)
      when(mockFileIO.createAndWriteFromResource(any[Path], any[String])).thenReturn(IO.unit)
      val sut = new HtmlReporter(mockFileIO)
      val report = MutationTestReport(thresholds = Thresholds(100, 0), files = Map.empty)
      val metrics = Metrics.calculateMetrics(report)

      sut.reportRunFinished(FinishedRunReport(report, metrics)).unsafeRunSync()

      val writtenFilesCaptor = ArgCaptor[Path]
      verify(mockFileIO, times(2)).createAndWrite(writtenFilesCaptor, any[String])
      verify(mockFileIO).createAndWriteFromResource(any[Path], eqTo(elementsLocation))
      val paths = writtenFilesCaptor.values.map(_.toString())
      all(paths) should fullyMatch regex stryker4sReportFolderRegex
      writtenFilesCaptor.values.map(_.getFileName().toString()) should contain only ("index.html", "report.js")
    }

    it("should write the mutation-test-elements.js file to the report directory") {
      val mockFileIO = mock[FileIO]
      when(mockFileIO.createAndWrite(any[Path], any[String])).thenReturn(IO.unit)
      when(mockFileIO.createAndWriteFromResource(any[Path], any[String])).thenReturn(IO.unit)
      val sut = new HtmlReporter(mockFileIO)
      val report = MutationTestReport(thresholds = Thresholds(100, 0), files = Map.empty)
      val metrics = Metrics.calculateMetrics(report)

      sut
        .reportRunFinished(FinishedRunReport(report, metrics))
        .unsafeRunSync()

      val elementsCaptor = ArgCaptor[Path]
      verify(mockFileIO, times(2)).createAndWrite(any[Path], any[String])
      verify(mockFileIO).createAndWriteFromResource(elementsCaptor, eqTo(elementsLocation))

      elementsCaptor.value.toString should fullyMatch regex stryker4sReportFolderRegex
      elementsCaptor.value.getFileName.toString() shouldEqual "mutation-test-elements.js"
    }

    it("should info log a message") {
      val mockFileIO = mock[FileIO]
      when(mockFileIO.createAndWrite(any[Path], any[String])).thenReturn(IO.unit)
      when(mockFileIO.createAndWriteFromResource(any[Path], any[String])).thenReturn(IO.unit)
      val sut = new HtmlReporter(mockFileIO)
      val report = MutationTestReport(thresholds = Thresholds(100, 0), files = Map.empty)
      val metrics = Metrics.calculateMetrics(report)

      sut.reportRunFinished(FinishedRunReport(report, metrics)).unsafeRunSync()

      val captor = ArgCaptor[Path]
      verify(mockFileIO).createAndWrite(captor.capture, eqTo(expectedHtml))
      verify(mockFileIO, times(2)).createAndWrite(any[Path], any[String])
      verify(mockFileIO).createAndWriteFromResource(any[Path], any[String])
      s"Written HTML report to ${captor.value}" shouldBe loggedAsInfo
    }
  }
}
