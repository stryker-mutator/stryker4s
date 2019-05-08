package stryker4s.report
import better.files.File
import org.mockito.captor.ArgCaptor
import org.mockito.{ArgumentMatchersSugar, MockitoSugar}
import stryker4s.config.Config
import stryker4s.files.{DiskFileIO, FileIO}
import stryker4s.model.MutantRunResults
import stryker4s.scalatest.LogMatchers
import stryker4s.testutil.Stryker4sSuite

import scala.concurrent.duration._
import scala.io.Source

class HtmlReporterTest extends Stryker4sSuite with MockitoSugar with ArgumentMatchersSugar with LogMatchers {

  describe("indexHtml") {
    it("should contain title") {
      implicit val config: Config = Config()
      val mockFileIO = mock[FileIO]
      val sut = new HtmlReporter(mockFileIO)

      val result = sut.indexHtml

      val expected =
        """<!DOCTYPE html>
          |<html>
          |<head>
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
      result.mkString should equal(expected)
    }
  }

  describe("reportJs") {
    it("should contain the report") {
      implicit val config: Config = Config()
      val mockFileIO = mock[FileIO]
      val sut = new HtmlReporter(mockFileIO)

      val result = sut.reportJs("{ 'foo': 'bar' }")

      val expected = "document.querySelector('mutation-test-report-app').report = { 'foo': 'bar' }"
      result.mkString should equal(expected)

    }
  }

  describe("mutation-test-elements") {
    it("should find the resource") {
      implicit val config: Config = Config()
      val fileIO = DiskFileIO

      val sut = new HtmlReporter(fileIO)

      val result = sut.testElementsJs()
      result.mkString.length should be > 50
    }
  }

  describe("reportRunFinished") {
    implicit val config: Config = Config()
    val stryker4sReportFolderRegex = ".*target(/|\\\\)stryker4s-report-(\\d*)(/|\\\\)[a-z]*\\.[a-z]*$"

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
      verify(mockFileIO).createAndWrite(elementsCaptor, any[Source])

      elementsCaptor.value.pathAsString should fullyMatch regex stryker4sReportFolderRegex
      elementsCaptor.value.name equals "mutation-test-elements.js"
    }

    it("should debug log a message") {
      val mockFileIO = mock[FileIO]
      val sut = new HtmlReporter(mockFileIO)
      val runResults = MutantRunResults(Nil, 50.0, 30.seconds)

      sut.reportRunFinished(runResults)

      "Written HTML report to " shouldBe loggedAsInfo
    }
  }
}
