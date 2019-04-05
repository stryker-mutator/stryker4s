package stryker4s.report
import grizzled.slf4j.Logging
import stryker4s.config.Config
import stryker4s.files.FileIO
import stryker4s.model.MutantRunResults
import stryker4s.report.mapper.MutantRunResultMapper

class HtmlReporter(fileIO: FileIO)(implicit config: Config)
    extends FinishedRunReporter
    with MutantRunResultMapper
    with Logging {

  private val htmlReportResource = s"mutation-testing-elements/mutation-test-elements.js"
  private val title = "Stryker4s report"
  private val reportFilename = "report.js"

  def indexHtml(): Iterator[Char] = {
    val mutationTestElementsScript = fileIO.readResource(htmlReportResource)

    val startHtml = s"""<!DOCTYPE html>
       |<html>
       |<body>
       |  <mutation-test-report-app title-postfix="$title">
       |    Your browser doesn't support <a href="https://caniuse.com/#search=custom%20elements">custom elements</a>.
       |    Please use a latest version of an evergreen browser (Firefox, Chrome, Safari, Opera, etc).
       |  </mutation-test-report-app>
       |  <script src="$reportFilename"></script>
       |  <script>
       |    """.stripMargin
    val endHtml = s"""
       |  </script>
       |</body>
       |</html>""".stripMargin

    startHtml.iterator ++
      mutationTestElementsScript ++
      endHtml
  }

  def reportJs(json: String): String =
    s"document.querySelector('mutation-test-report-app').report = $json"

  override def reportRunFinished(runResults: MutantRunResults): Unit = {
    val mapped = toReport(runResults).toJson

    val targetLocation = config.baseDir / s"target/stryker4s-report-${System.currentTimeMillis()}"
    val indexLocation = targetLocation / "index.html"
    val reportLocation = targetLocation / reportFilename
    val indexContent = indexHtml()
    val reportContent = reportJs(mapped)
    fileIO.createAndWrite(indexLocation, indexContent)
    fileIO.createAndWrite(reportLocation, reportContent)

    debug(s"Written HTML report to $targetLocation")
  }
}
