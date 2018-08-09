package stryker4s.run.report.mapper
import java.nio.file.Path

import stryker4s.config.Config
import stryker4s.model._

trait MutantRunResultMapper {

  def toHtmlMutantRunResult(runResults: MutantRunResults)(
      implicit config: Config): HtmlMutantRunResults = {
    val detected = runResults.results collect { case d: Detected => d }
    val detectedSize = detected.size

    val undetected = runResults.results collect { case u: Undetected => u }
    val undetectedSize = undetected.size

    val totalMutants = detectedSize + undetectedSize

    runResults.results.map {
      case e: Killed => e
      case _         => println("not intrested")
    }.size

    val sortedOnFile: Map[Path, Iterable[MutantRunResult]] =
      runResults.results.groupBy(result => result.fileSubPath)

    val htmlRunResults: List[HtmlMutantRunResult] = sortedOnFile.map {
      case (path, results) => {
        HtmlMutantRunResult(
          path.getFileName.toString,
          path.toAbsolutePath.toString,
          null,
          calculateHealth(results),
          "scala",
          "",
          results
            .map(result => {
              HtmlMutant(result.mutant.id.toString,
                         "Not yet",
                         result.mutant.mutated.toString(),
                         calculateSpan(result.mutant),
                         result.getClass.getSimpleName)
            })
            .toList
        )
      }
    }.toList

    HtmlMutantRunResults("TODO",
                         config.baseDir.path.toString,
                         null,
                         calculateHealth(runResults.results),
                         htmlRunResults)
  }

  private[this] def calculateSpan(mutant: Mutant): Array[Int] = {
    Array(mutant.original.pos.startColumn, mutant.original.pos.endColumn)
  }

  private[this] def calculateHealth(results: Iterable[MutantRunResult]): String = {
    val detected: Long = results.collect { case d: Detected => d }.size
    val mutationScore = calculateMutationScore(results.size, detected)

    mutationScore match {
      case score if 80 to 100 contains score   => "ok"
      case score if 50 until 80 contains score => "warning"
      case score if 0 until 50 contains score  => "danger"
    }
  }

  private[this] def calculateMutationScore(totalMutants: Double,
                                           detectedMutants: Double): Double = {
    val mutationScore = detectedMutants / totalMutants * 100
    BigDecimal(mutationScore).setScale(2, BigDecimal.RoundingMode.HALF_UP).toDouble
  }
}
