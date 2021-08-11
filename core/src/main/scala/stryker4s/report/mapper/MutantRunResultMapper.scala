package stryker4s.report.mapper

import fs2.io.file.Path
import mutationtesting._
import stryker4s.config.{Config, Thresholds => ConfigThresholds}
import stryker4s.model._

import java.nio.file.Files

trait MutantRunResultMapper {
  protected[report] def toReport(
      results: Map[Path, Seq[MutantRunResult]]
  )(implicit config: Config): MutationTestResult[Config] =
    MutationTestResult(
      thresholds = toThresholds(config.thresholds),
      files = toFileResultMap(results),
      projectRoot = Some(config.baseDir.absolute.toString),
      config = Some(config)
    )

  private def toThresholds(thresholds: ConfigThresholds): Thresholds =
    Thresholds(high = thresholds.high, low = thresholds.low)

  private def toFileResultMap(
      results: Map[Path, Seq[MutantRunResult]]
  )(implicit config: Config): Map[String, FileResult] =
    results.map { case (path, runResults) =>
      path.toString.replace('\\', '/') -> toFileResult(path, runResults)
    }

  private def toFileResult(path: Path, runResults: Seq[MutantRunResult])(implicit
      config: Config
  ): FileResult =
    FileResult(
      fileContentAsString(path),
      runResults.map(toMutantResult)
    )

  private def toMutantResult(runResult: MutantRunResult): MutantResult = {
    val mutant = runResult.mutant
    MutantResult(
      mutant.id.toString,
      mutant.mutationType.mutationName,
      mutant.mutated.syntax,
      toLocation(mutant.original.pos),
      toMutantStatus(runResult),
      runResult.description
    )
  }

  private def toLocation(pos: scala.meta.inputs.Position): Location =
    Location(
      start = Position(line = pos.startLine + 1, column = pos.startColumn + 1),
      end = Position(line = pos.endLine + 1, column = pos.endColumn + 1)
    )

  private def toMutantStatus(mutant: MutantRunResult): MutantStatus =
    mutant match {
      case _: Survived   => MutantStatus.Survived
      case _: Killed     => MutantStatus.Killed
      case _: NoCoverage => MutantStatus.NoCoverage
      case _: TimedOut   => MutantStatus.Timeout
      case _: Error      => MutantStatus.RuntimeError
      case _: Ignored    => MutantStatus.Ignored
    }

  private def fileContentAsString(path: Path)(implicit config: Config): String =
    Files.readString((config.baseDir / path.toString).toNioPath)
}
