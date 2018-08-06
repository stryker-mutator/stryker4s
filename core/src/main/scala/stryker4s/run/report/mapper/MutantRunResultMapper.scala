package stryker4s.run.report.mapper
import stryker4s.config.Config
import stryker4s.model.{Detected, MutantRunResults, Undetected}

trait MutantRunResultMapper {

  def toHtmlMutantRunResult(runResults: MutantRunResults, config: Config): HtmlMutantRunResults = {
    val detected = runResults.results collect { case d: Detected => d }
    val detectedSize = detected.size

    val undetected = runResults.results collect { case u: Undetected => u }
    val undetectedSize = undetected.size

    val totalMutants = detectedSize + undetectedSize

    val htmlMutantRunResults: List[HtmlMutantRunResult] = runResults.results
      .map(
        result =>
          HtmlMutantRunResult(
            "",
            result.fileSubPath.toString,
            Totals(detectedSize, undetectedSize, 0, 0),
            "",
            "scala",
            result.mutant.original.toString(),
            List(
              HtmlMutant("",
                         "",
                         result.mutant.mutated.toString(),
                         Array(result.mutant.original.pos.startColumn,
                               result.mutant.original.pos.endColumn),
                         ""))
        ))
      .toList
    HtmlMutantRunResults("TODO", config.baseDir.path.toString, null, htmlMutantRunResults)

  }
}
