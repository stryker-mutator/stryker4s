package stryker4s.report
import better.files.Resource
import stryker4s.config.Config
import stryker4s.model.MutantRunResults
import stryker4s.report.mapper.MutantRunResultMapper

class HtmlReporter extends MutantRunReporter with MutantRunResultMapper {

  private val reportVersion = "0.0.7"

  def indexHtml(json: String): String = {
    val mutationTestElementsScript = Resource.getAsString(
      s"META-INF/resources/webjars/mutation-testing-elements/$reportVersion/dist/mutation-test-elements.js")

    s"""<!DOCTYPE html>
       |<html>
       |<body>
       |  <mutation-test-report-app title-postfix="Stryker4s report"></mutation-test-report-app>
       |  <script>
       |    document.querySelector('mutation-test-report-app').report = $json
       |  </script>
       |  <script>
       |    $mutationTestElementsScript
       |  </script>
       |</body>
       |</html>""".stripMargin
  }

  override def report(runResults: MutantRunResults)(implicit config: Config): Unit = {
    val mapped = toReport(runResults).toJson

    println(mapped)
  }
}
