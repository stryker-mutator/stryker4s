package stryker4s.run.report.mapper
import java.nio.file.{Path, Paths}

import better.files.File
import stryker4s.config.Config
import stryker4s.model._

trait MutantRunResultMapper {

  def toHtmlMutantRunResult(runResults: MutantRunResults)(implicit config: Config): HtmlMutantRunResults = {
    val detectedSize = runResults.results.collect { case d: Detected     => d }.size
    val undetectedSize = runResults.results.collect { case u: Undetected => u }.size

    val sortedOnFile: Map[Path, Iterable[MutantRunResult]] =
      runResults.results.groupBy(result => result.fileSubPath)

    val htmlRunResults: List[HtmlMutantRunResult] = sortedOnFile.map {
      case (path, results) =>
        val detectedSize = results.collect { case d: Detected     => d }.size
        val undetectedSize = results.collect { case d: Undetected => d }.size

        HtmlMutantRunResult(
          path.getFileName.toString,
          path.toAbsolutePath.toString,
          Totals(detectedSize, undetectedSize, 0, 0),
          calculateHealth(results),
          "scala",
          getFileSourceAsString(path),
          results
            .map(result => {
              HtmlMutant(
                result.mutant.id.toString,
                result.mutant.mutationType.mutationName,
                result.mutant.mutated.syntax,
                calculateSpan(result.mutant).toList.toString(),
                result.getClass.getSimpleName
              )
            })
            .toList
        )
    }.toList

    HtmlMutantRunResults("TODO",
                         config.baseDir.path.toString,
                         Totals(detectedSize, undetectedSize, 0, 0),
                         calculateHealth(runResults.results),
                         htmlRunResults)
  }

  private[this] def calculateSpan(mutant: Mutant): Array[Int] = {
    Array(mutant.original.pos.startColumn, mutant.original.pos.endColumn)
  }

  private[this] def calculateHealth(results: Iterable[MutantRunResult]): String = {
    val detected: Long = results.collect { case d: Detected => d }.size
    val mutationScore: Double = calculateMutationScore(results.size, detected)

    mutationScore match {
      case score if isInOkRange(score)      => "ok"
      case score if isInWarningRange(score) => "warning"
      case score if isInDangerRange(score)  => "danger"
    }
  }

  private[this] def isInOkRange(score: Double) = isBetween(score, 80.00, 100.00)
  private[this] def isInWarningRange(score: Double) = isBetween(score, 50.00, 79.99)
  private[this] def isInDangerRange(score: Double) = isBetween(score, 0.00, 49.99)

  private[this] def isBetween(score: Double, lowerBound: Double, upperBound: Double): Boolean = {
    score >= lowerBound && score <= upperBound
  }

  private[this] def calculateMutationScore(totalMutants: Double, detectedMutants: Double): Double = {
    val mutationScore = detectedMutants / totalMutants * 100
    BigDecimal(mutationScore).setScale(2, BigDecimal.RoundingMode.HALF_UP).toDouble
  }

  private[this] def getFileSourceAsString(path: Path)(implicit config: Config): String = {
    File(Paths.get(config.baseDir.path.toString, path.toString)).contentAsString
  }
}
