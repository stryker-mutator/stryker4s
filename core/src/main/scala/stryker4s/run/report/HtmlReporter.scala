package stryker4s.run.report
import stryker4s.config.Config
import stryker4s.model.MutantRunResults
import stryker4s.run.report.mapper.{HtmlMutantRunResults, MutantRunResultMapper}

class HtmlReporter extends MutantRunReporter with MutantRunResultMapper {

  override def report(runResults: MutantRunResults)(implicit config: Config): Unit = {
    val mapped: HtmlMutantRunResults = toHtmlMutantRunResult(runResults)



      println(mapped)
  }
}
