package stryker4s.report.mapper

import fs2.io.file.Path
import mutationtesting.*
import stryker4s.config.{Config, Thresholds as ConfigThresholds}
import stryker4s.model.*
import stryker4s.model.MutantResultsPerFile

import java.nio.file.Files

trait MutantRunResultMapper {
  protected[stryker4s] def toReport(
      results: MutantResultsPerFile
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
      results: MutantResultsPerFile
  )(implicit config: Config): Map[String, FileResult] =
    results.map { case (path, runResults) =>
      path.toString.replace('\\', '/') -> toFileResult(path, runResults)
    }

  private def toFileResult(path: Path, runResults: Seq[MutantResult])(implicit
      config: Config
  ): FileResult =
    FileResult(
      fileContentAsString(path),
      runResults
    )

  // TODO: remove?
  def toLocation(pos: scala.meta.inputs.Position): Location =
    Location(
      start = Position(line = pos.startLine + 1, column = pos.startColumn + 1),
      end = Position(line = pos.endLine + 1, column = pos.endColumn + 1)
    )

  // TODO: remove?
  def toMutantStatus(mutant: MutantRunResult): MutantStatus =
    mutant match {
      case _: Survived     => MutantStatus.Survived
      case _: Killed       => MutantStatus.Killed
      case _: NoCoverage   => MutantStatus.NoCoverage
      case _: TimedOut     => MutantStatus.Timeout
      case _: Error        => MutantStatus.RuntimeError
      case _: Ignored      => MutantStatus.Ignored
      case _: CompileError => MutantStatus.CompileError
    }

  private def fileContentAsString(path: Path)(implicit config: Config): String =
    new String(Files.readAllBytes((config.baseDir / path).toNioPath))
}
