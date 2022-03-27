package stryker4s.report.mapper

import fs2.io.file.Path
import mutationtesting.*
import stryker4s.config.{Config, Thresholds as ConfigThresholds}
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
  ): Map[String, FileResult] =
    results.map { case (path, runResults) =>
      path.toString.replace('\\', '/') -> toFileResult(path, runResults)
    }

  private def toFileResult(path: Path, runResults: Seq[MutantResult]): FileResult =
    FileResult(
      fileContentAsString(path),
      runResults
    )

  private def fileContentAsString(path: Path): String =
    new String(Files.readAllBytes(path.toNioPath))
}
