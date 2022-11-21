package stryker4jvm.reporting

import cats.effect.IO
import fs2.*
import fs2.io.file.{Files, Path}
import mutationtesting.{Metrics, MutationTestResult, Thresholds}
import org.mockito.captor.ArgCaptor
import stryker4jvm.files.{DiskFileIO, FileIO}
import stryker4jvm.scalatest.LogMatchers
import stryker4jvm.testutil.{MockitoIOSuite, Stryker4jvmIOSuite}

import scala.concurrent.duration.*

class HtmlReporterTest extends Stryker4jvmIOSuite with MockitoIOSuite with LogMatchers {

  private val elementsLocation = "/elements/mutation-test-elements.js"

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
      |  <script>
      |    const app = document.getElementsByTagName('mutation-test-report-app').item(0);
      |    function updateTheme() {
      |      document.body.style.backgroundColor = app.themeBackgroundColor;
      |    }
      |    app.addEventListener('theme-changed', updateTheme);
      |    updateTheme();
      |  </script>
      |  <script src="report.js"></script>
      |</body>
      |</html>""".stripMargin

  describe("indexHtml") {
    it("should contain title") {
      val mockFileIO = mock[FileIO]
      whenF(mockFileIO.createAndWrite(any[Path], any[String])).thenReturn(())
      val sut = new HtmlReporter(mockFileIO)
      val testFile = Path("foo.bar")

      sut
        .writeIndexHtmlTo(testFile)
        .map { _ =>
          verify(mockFileIO).createAndWrite(testFile, expectedHtml)
        }
        .assertNoException
    }
  }

  describe("reportJs") {
    it("should contain the report") {
      val mockFileIO = mock[FileIO]
      whenF(mockFileIO.createAndWrite(any[Path], any[String])).thenReturn(())
      val sut = new HtmlReporter(mockFileIO)
      val testFile = Path("foo.bar")
      val runResults = MutationTestResult(thresholds = Thresholds(100, 0), files = Map.empty)

      sut
        .writeReportJsTo(testFile, runResults)
        .map { _ =>
          val expectedJs =
            """document.querySelector('mutation-test-report-app').report = {"$schema":"https://git.io/mutation-testing-schema","schemaVersion":"1","thresholds":{"high":100,"low":0},"files":{}}"""
          verify(mockFileIO).createAndWrite(testFile, expectedJs)
        }
        .assertNoException
    }
  }

  describe("mutation-test-elements") {
    it("should write the resource") {
      // Arrange
      val fileIO = new DiskFileIO()

      Files[IO].tempDirectory.use { tmpDir =>
        val tempFile = tmpDir.resolve("mutation-test-elements.js")
        val sut = new HtmlReporter(fileIO)

        // Act
        sut
          .writeMutationTestElementsJsTo(tempFile) >> {
          // assert
          val atLeastSize: Long = 100 * 1024L // 100KB
          Files[IO].size(tempFile).asserting(_ should be > atLeastSize)
        } >> {
          val expectedHeader = "/*! For license information please see mutation-test-elements.js.LICENSE.txt */"
          // Read the first line
          Files[IO]
            .readRange(tempFile, 256, 0, expectedHeader.getBytes().length.toLong)
            .through(text.utf8.decode)
            .head
            .compile
            .lastOrError
            .asserting(
              _ shouldBe expectedHeader
            )
        }
      }
    }
  }

  describe("onRunFinished") {
    val fileLocation = Path("target") / "stryker4s-report"

    it("should write the report files to the report directory") {
      val mockFileIO = mock[FileIO]
      whenF(mockFileIO.createAndWrite(any[Path], any[String])).thenReturn(())
      whenF(mockFileIO.createAndWriteFromResource(any[Path], any[String])).thenReturn(())
      val sut = new HtmlReporter(mockFileIO)
      val report = MutationTestResult(thresholds = Thresholds(100, 0), files = Map.empty)
      val metrics = Metrics.calculateMetrics(report)

      sut
        .onRunFinished(FinishedRunEvent(report, metrics, 0.seconds, fileLocation))
        .asserting { _ =>
          val writtenFilesCaptor = ArgCaptor[Path]
          verify(mockFileIO, times(2)).createAndWrite(writtenFilesCaptor, any[String])
          verify(mockFileIO).createAndWriteFromResource(any[Path], eqTo(elementsLocation))
          val paths = writtenFilesCaptor.values.map(_.toString)
          all(paths) should include(fileLocation.toString)
          writtenFilesCaptor.values.map(_.fileName.toString) should contain.only("index.html", "report.js")
        }
    }

    it("should write the mutation-test-elements.js file to the report directory") {
      val mockFileIO = mock[FileIO]
      whenF(mockFileIO.createAndWrite(any[Path], any[String])).thenReturn(())
      whenF(mockFileIO.createAndWriteFromResource(any[Path], any[String])).thenReturn(())
      val sut = new HtmlReporter(mockFileIO)
      val report = MutationTestResult(thresholds = Thresholds(100, 0), files = Map.empty)
      val metrics = Metrics.calculateMetrics(report)

      sut
        .onRunFinished(FinishedRunEvent(report, metrics, 10.seconds, fileLocation))
        .asserting { _ =>
          val elementsCaptor = ArgCaptor[Path]
          verify(mockFileIO, times(2)).createAndWrite(any[Path], any[String])
          verify(mockFileIO).createAndWriteFromResource(elementsCaptor, eqTo(elementsLocation))

          val expectedFileLocation = fileLocation / "mutation-test-elements.js"
          elementsCaptor.value.toString should endWith(expectedFileLocation.toString)
        }
    }

    it("should info log a message") {
      val mockFileIO = mock[FileIO]
      whenF(mockFileIO.createAndWrite(any[Path], any[String])).thenReturn(())
      whenF(mockFileIO.createAndWriteFromResource(any[Path], any[String])).thenReturn(())
      val sut = new HtmlReporter(mockFileIO)
      val report = MutationTestResult(thresholds = Thresholds(100, 0), files = Map.empty)
      val metrics = Metrics.calculateMetrics(report)

      sut
        .onRunFinished(FinishedRunEvent(report, metrics, 10.seconds, fileLocation))
        .asserting { _ =>
          val captor = ArgCaptor[Path]
          verify(mockFileIO).createAndWrite(captor.capture, eqTo(expectedHtml))
          verify(mockFileIO, times(2)).createAndWrite(any[Path], any[String])
          verify(mockFileIO).createAndWriteFromResource(any[Path], any[String])
          s"Written HTML report to ${(fileLocation / "index.html").toString}" shouldBe loggedAsInfo
        }
    }
  }
}
