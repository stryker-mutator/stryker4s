package stryker4s.report.mapper
import java.nio.file.Path

import stryker4s.config.{Config, Thresholds => ConfigThresholds}
import stryker4s.model._
import stryker4s.report.model.MutantStatus.MutantStatus
import stryker4s.report.model._

trait MutantRunResultMapper {

  private val schemaVersion = "1"

  def toReport(mutantRunResults: MutantRunResults)(implicit config: Config): MutationTestReport = MutationTestReport(
    schemaVersion,
    toThreshold(config.thresholds),
    toFiles(mutantRunResults.results.toSeq)
  )

  def toThreshold(thresholds: ConfigThresholds): Thresholds =
    Thresholds(high = thresholds.high, low = thresholds.low)

  def toFiles(results: Seq[MutantRunResult])(implicit config: Config): Map[String, MutationTestResult] =
    results groupBy (_.fileSubPath) map {
      case (path, runResults) => path.toString.replace('\\', '/') -> toMutationTestResults(runResults)
    }

  def toMutationTestResults(runResults: Seq[MutantRunResult])(implicit config: Config): MutationTestResult =
    MutationTestResult(
      fileContentAsString(runResults.head.fileSubPath),
      runResults.map(toMutantRunResult)
    )

  def toMutantRunResult(runResult: MutantRunResult): MutantResult = {
    val mutant = runResult.mutant
    MutantResult(
      mutant.id.toString,
      mutant.mutationType.mutationName,
      mutant.mutated.syntax,
      toLocation(mutant.original.pos),
      toMutantStatus(runResult)
    )
  }

  def toLocation(pos: scala.meta.inputs.Position): Location = Location(
    start = Position(line = pos.startLine + 1, column = pos.startColumn + 1),
    end = Position(line = pos.endLine + 1, column = pos.endColumn + 1)
  )

  def toMutantStatus(mutant: MutantRunResult): MutantStatus = mutant match {
    case _: Survived   => MutantStatus.Survived
    case _: Killed     => MutantStatus.Killed
    case _: NoCoverage => MutantStatus.NoCoverage
    case _: TimedOut   => MutantStatus.Timeout
    case _: Error      => MutantStatus.CompileError
  }

  private[this] def fileContentAsString(path: Path)(implicit config: Config): String =
    (config.baseDir / path.toString).contentAsString

}
