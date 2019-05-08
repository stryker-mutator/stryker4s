package stryker4s.report

import better.files.File
import grizzled.slf4j.Logging
import stryker4s.config.Config
import stryker4s.files.FileIO
import stryker4s.model.MutantRunResults
import stryker4s.report.mapper.MutantRunResultMapper
import stryker4s.report.model.MutationTestReport

class HtmlReporter(fileIO: FileIO)(implicit config: Config)
    extends FinishedRunReporter
    with MutantRunResultMapper
    with Logging {

  private val title = "Stryker4s report"
  private val mutationTestElementsName = "mutation-test-elements.js"
  private val htmlReportResource = s"mutation-testing-elements/$mutationTestElementsName"
  private val reportFilename = "report.js"

  private val indexHtml: String =
    s"""<!DOCTYPE html>
       |<html lang="en">
       |<head>
       |  <meta charset="UTF-8">
       |  <meta name="viewport" content="width=device-width, initial-scale=1.0">
       |  <script src="mutation-test-elements.js"></script>
       |</head>
       |<body>
       |  <mutation-test-report-app title-postfix="$title">
       |    Your browser doesn't support <a href="https://caniuse.com/#search=custom%20elements">custom elements</a>.
       |    Please use a latest version of an evergreen browser (Firefox, Chrome, Safari, Opera, etc).
       |  </mutation-test-report-app>
       |  <script src="$reportFilename"></script>
       |</body>
       |</html>""".stripMargin

  def writeMutationTestElementsJsTo(file: File): Unit =
    fileIO.createAndWriteFromResource(file, htmlReportResource)

  def writeIndexHtmlTo(file: File): Unit =
    fileIO.createAndWrite(file, indexHtml)

  def writeReportJsTo(file: File, report: MutationTestReport): Unit = {
    val json = report.toJson
    val reportContent = s"document.querySelector('mutation-test-report-app').report = $json"
    fileIO.createAndWrite(file, reportContent)
  }

  override def reportRunFinished(runResults: MutantRunResults): Unit = {
    val targetLocation = config.baseDir / s"target/stryker4s-report-${System.currentTimeMillis()}"

    val mutationTestElementsLocation = targetLocation / mutationTestElementsName
    val indexLocation = targetLocation / "index.html"
    val reportLocation = targetLocation / reportFilename

    writeIndexHtmlTo(indexLocation)
    writeReportJsTo(reportLocation, toReport(runResults))
    writeMutationTestElementsJsTo(mutationTestElementsLocation)

    info(s"Written HTML report to $indexLocation")
  }
}
