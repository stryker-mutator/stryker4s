package stryker4s.report
import better.files.File
import org.mockito.captor.ArgCaptor
import org.mockito.{ArgumentMatchersSugar, MockitoSugar}
import stryker4s.config.Config
import stryker4s.files.{DiskFileIO, FileIO}
import stryker4s.model.MutantRunResults
import stryker4s.report.model.{MutationTestReport, Thresholds}
import stryker4s.scalatest.LogMatchers
import stryker4s.testutil.Stryker4sSuite

import scala.concurrent.duration._

class HtmlReporterTest extends Stryker4sSuite with MockitoSugar with ArgumentMatchersSugar with LogMatchers {

  describe("indexHtml") {
    it("should contain title") {
      implicit val config: Config = Config()
      val mockFileIO = mock[FileIO]
      val sut = new HtmlReporter(mockFileIO)
      val testFile = config.baseDir / "foo.bar"

      sut.writeIndexHtmlTo(testFile)

      val expected =
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
      verify(mockFileIO).createAndWrite(testFile, expected)
    }
  }

  describe("reportJs") {
    it("should contain the report") {
      implicit val config: Config = Config()
      val mockFileIO = mock[FileIO]
      val sut = new HtmlReporter(mockFileIO)
      val testFile = config.baseDir / "foo.bar"
      val runResults = MutationTestReport("1.0", Thresholds(100, 0), Map.empty)

      sut.writeReportJsTo(testFile, runResults)

      val expectedJs =
        """document.querySelector('mutation-test-report-app').report = {"schemaVersion":"1.0","thresholds":{"high":100,"low":0},"files":{}}"""
      verify(mockFileIO).createAndWrite(testFile, expectedJs)
    }
  }

  describe("mutation-test-elements") {
    it("should write the resource") {
      implicit val config: Config = Config()
      val fileIO = DiskFileIO
      File.usingTemporaryFile() { tempFile =>
        val sut = new HtmlReporter(fileIO)

        sut.writeMutationTestElementsJsTo(tempFile)
        val atLeastSize: Long = 200 * 1024 // 200KB
        tempFile.size should be > atLeastSize
        tempFile.lineIterator.next() should startWith("!function(")
      }
    }
  }

  describe("reportRunFinished") {
    implicit val config: Config = Config()
    val stryker4sReportFolderRegex = ".*target(/|\\\\)stryker4s-report-(\\d*)(/|\\\\)[a-z-]*\\.[a-z]*$"

    it("should write the report files to the report directory") {
      val mockFileIO = mock[FileIO]
      val sut = new HtmlReporter(mockFileIO)
      val runResults = MutantRunResults(Nil, 50.0, 30.seconds)

      sut.reportRunFinished(runResults)

      val writtenFilesCaptor = ArgCaptor[File]

      verify(mockFileIO, times(2)).createAndWrite(writtenFilesCaptor, any[String])

      val paths = writtenFilesCaptor.values.map(_.pathAsString)
      all(paths) should fullyMatch regex stryker4sReportFolderRegex

      writtenFilesCaptor.values.map(_.name) should contain only ("index.html", "report.js")
    }

    it("should write the mutation-test-elements.js file to the report directory") {
      val mockFileIO = mock[FileIO]
      val sut = new HtmlReporter(mockFileIO)
      val runResults = MutantRunResults(Nil, 50.0, 30.seconds)

      sut.reportRunFinished(runResults)

      val elementsCaptor = ArgCaptor[File]
      verify(mockFileIO).createAndWriteFromResource(elementsCaptor,
                                                    eqTo("mutation-testing-elements/mutation-test-elements.js"))

      elementsCaptor.value.pathAsString should fullyMatch regex stryker4sReportFolderRegex
      elementsCaptor.value.name equals "mutation-test-elements.js"
    }

    it("should info log a message") {
      val mockFileIO = mock[FileIO]
      val sut = new HtmlReporter(mockFileIO)
      val runResults = MutantRunResults(Nil, 50.0, 30.seconds)

      sut.reportRunFinished(runResults)

      "Written HTML report to " shouldBe loggedAsInfo
    }
  }
}
