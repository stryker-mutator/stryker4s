package stryker4s.report

import cats.effect.IO
import cats.syntax.all.*
import fs2.*
import fs2.io.file.{Files, Path}
import mutationtesting.{Metrics, MutationTestResult, Thresholds}
import stryker4s.files.DiskFileIO
import stryker4s.testkit.{LogMatchers, Stryker4sIOSuite}
import stryker4s.testutil.stubs.FileIOStub

import scala.concurrent.duration.*
import stryker4s.config.Config
import stryker4s.testutil.stubs.DesktopIOStub

class HtmlReporterTest extends Stryker4sIOSuite with LogMatchers {

  implicit val config: Config = Config.default

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
    test("should contain title") {
      val fileIOStub = FileIOStub()
      val desktopIOStub = DesktopIOStub()
      val sut = new HtmlReporter(fileIOStub, desktopIOStub)
      val testFile = Path("foo.bar")

      sut
        .writeIndexHtmlTo(testFile) >>
        fileIOStub.createAndWriteCalls.asserting { calls =>
          assertEquals(calls.loneElement._1, testFile)
          assertEquals(calls.loneElement._2, expectedHtml)
        }
    }
  }

  describe("reportJs") {
    test("should contain the report") {
      val fileIOStub = FileIOStub()
      val desktopIOStub = DesktopIOStub()
      val sut = new HtmlReporter(fileIOStub, desktopIOStub)
      val testFile = Path("foo.bar")
      val runResults = MutationTestResult(thresholds = Thresholds(100, 0), files = Map.empty)

      sut
        .writeReportJsTo(testFile, runResults) >>
        fileIOStub.createAndWriteCalls.asserting { calls =>
          val expectedJs =
            """document.querySelector('mutation-test-report-app').report = {"$schema":"https://git.io/mutation-testing-schema","schemaVersion":"2","thresholds":{"high":100,"low":0},"files":{}}"""

          assertEquals(calls.loneElement, (testFile, expectedJs))
        }
    }
  }

  describe("mutation-test-elements") {
    test("should write the resource") {
      // Arrange
      val fileIO = new DiskFileIO()
      val desktopIOStub = DesktopIOStub()
      Files[IO].tempDirectory.use { tmpDir =>
        val tempFile = tmpDir.resolve("mutation-test-elements.js")
        val sut = new HtmlReporter(fileIO, desktopIOStub)

        // Act
        sut
          .writeMutationTestElementsJsTo(tempFile) >> {
          // assert
          val atLeastSize: Long = 100 * 1024L // 100KB
          Files[IO].size(tempFile).map(size => assert(size > atLeastSize))
        } >> {
          val expectedHeader = """var MutationTestElements"""
          // Read the first line
          Files[IO]
            .readRange(tempFile, 256, 0, expectedHeader.getBytes().length.toLong)
            .through(text.utf8.decode)
            .head
            .compile
            .lastOrError
            .assertEquals(expectedHeader)
        }
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
        (fileIOStub.createAndWriteFromResourceCalls, fileIOStub.createAndWriteCalls).tupled.asserting {
          case (fromResourceCalls, createAndWriteCalls) =>
            createAndWriteCalls
              .map(_._1.toString)
              .foreach(fileName => assert(fileName.contains(fileLocation.toString), fileName))
            assertSameElements(createAndWriteCalls.map(_._1.fileName.toString), Seq("index.html", "report.js"))
            assertEquals(fromResourceCalls.loneElement._2, elementsLocation)
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
        fileIOStub.createAndWriteFromResourceCalls.asserting { calls =>
          val expectedFileLocation = fileLocation / "mutation-test-elements.js"
          assertEquals(calls.loneElement._1, expectedFileLocation)
          assertEquals(calls.loneElement._2, elementsLocation)
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

    test("should open the report automatically") {
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
          val expectedFile = expectedFileLocation.toNioPath.toFile()
          assertEquals(calls.loneElement, expectedFile)
        }
    }
  }
}
