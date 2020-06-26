package stryker4s.report
import better.files.File
import mutationtesting.{Metrics, MutationTestReport, Thresholds}
import org.mockito.captor.ArgCaptor
import stryker4s.config.Config
import stryker4s.files.{DiskFileIO, FileIO}
import stryker4s.scalatest.LogMatchers
import stryker4s.testutil.{AsyncStryker4sSuite, MockitoSuite}
import java.nio.file.Path

class HtmlReporterTest extends AsyncStryker4sSuite with MockitoSuite with LogMatchers {
  private val elementsLocation = "mutation-testing-elements/mutation-test-elements.js"

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
      val sut = new HtmlReporter(mockFileIO)
      val testFile = (config.baseDir / "foo.bar").path

      (for {
        _ <- sut.writeIndexHtmlTo(testFile)
        _ <- verify(mockFileIO).createAndWrite(testFile, expectedHtml)
      } yield succeed).unsafeToFuture()
    }
  }

  describe("reportJs") {
    it("should contain the report") {
      implicit val config: Config = Config.default
      val mockFileIO = mock[FileIO]
      val sut = new HtmlReporter(mockFileIO)
      val testFile = (config.baseDir / "foo.bar").path
      val runResults = MutationTestReport(thresholds = Thresholds(100, 0), files = Map.empty)

      (for {
        _ <- sut.writeReportJsTo(testFile, runResults)

        expectedJs =
          """document.querySelector('mutation-test-report-app').report = {"$schema":"https://raw.githubusercontent.com/stryker-mutator/mutation-testing-elements/master/packages/mutation-testing-report-schema/src/mutation-testing-report-schema.json","schemaVersion":"1","thresholds":{"high":100,"low":0},"files":{}}"""
        _ <- verify(mockFileIO).createAndWrite(testFile, expectedJs)
      } yield succeed).unsafeToFuture()
    }
  }

  describe("mutation-test-elements") {
    it("should write the resource") {
      implicit val config: Config = Config.default
      val fileIO = new DiskFileIO()

      val tempFile = File.temp
      val sut = new HtmlReporter(fileIO)

      (sut.writeMutationTestElementsJsTo(tempFile.path) map { _ =>
        val atLeastSize: Long = 100 * 1024L // 100KB
        tempFile.size should be > atLeastSize
        tempFile.lineIterator
          .next() shouldEqual "/*! For license information please see mutation-test-elements.js.LICENSE.txt */"
      }).unsafeToFuture()
    }
  }

  describe("reportRunFinished") {
    implicit val config: Config = Config.default
    val stryker4sReportFolderRegex = ".*target(/|\\\\)stryker4s-report-(\\d*)(/|\\\\)[a-z-]*\\.[a-z]*$"

    it("should write the report files to the report directory") {
      val mockFileIO = mock[FileIO]
      val sut = new HtmlReporter(mockFileIO)
      val report = MutationTestReport(thresholds = Thresholds(100, 0), files = Map.empty)
      val metrics = Metrics.calculateMetrics(report)

      (for {
        _ <- sut.reportRunFinished(FinishedRunReport(report, metrics))

        writtenFilesCaptor = ArgCaptor[Path]

        _ <- verify(mockFileIO, times(2)).createAndWrite(writtenFilesCaptor, any[String])
        _ <- verify(mockFileIO).createAndWriteFromResource(any[Path], eqTo(elementsLocation))

        paths = writtenFilesCaptor.values.map(_.toString())
        _ = all(paths) should fullyMatch regex stryker4sReportFolderRegex

        assertion =
          writtenFilesCaptor.values.map(_.getFileName().toString()) should contain(only("index.html", "report.js"))
      } yield assertion).unsafeToFuture()
    }

    it("should write the mutation-test-elements.js file to the report directory") {
      val mockFileIO = mock[FileIO]
      val sut = new HtmlReporter(mockFileIO)
      val report = MutationTestReport(thresholds = Thresholds(100, 0), files = Map.empty)
      val metrics = Metrics.calculateMetrics(report)

      (for {
        _ <- sut.reportRunFinished(FinishedRunReport(report, metrics))

        elementsCaptor = ArgCaptor[Path]
        _ <- verify(mockFileIO, times(2)).createAndWrite(any[Path], any[String])
        _ <- verify(mockFileIO).createAndWriteFromResource(elementsCaptor, eqTo(elementsLocation))

        _ = elementsCaptor.value.toString should fullyMatch regex stryker4sReportFolderRegex
        assertion = elementsCaptor.value.getFileName shouldEqual "mutation-test-elements.js"
      } yield assertion).unsafeToFuture()
    }

    it("should info log a message") {
      val mockFileIO = mock[FileIO]
      val sut = new HtmlReporter(mockFileIO)
      val report = MutationTestReport(thresholds = Thresholds(100, 0), files = Map.empty)
      val metrics = Metrics.calculateMetrics(report)

      (for {
        _ <- sut.reportRunFinished(FinishedRunReport(report, metrics))

        captor = ArgCaptor[Path]
        _ <- verify(mockFileIO).createAndWrite(captor.capture, eqTo(expectedHtml))
        _ <- verify(mockFileIO, times(2)).createAndWrite(any[Path], any[String])
        _ <- verify(mockFileIO).createAndWriteFromResource(any[Path], any[String])
        assertion = s"Written HTML report to ${captor.value}" shouldBe loggedAsInfo
      } yield assertion).unsafeToFuture()
    }
  }
}
