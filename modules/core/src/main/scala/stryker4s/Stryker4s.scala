package stryker4s

import cats.effect.IO
import cats.syntax.align.*
import mutationtesting.Metrics
import stryker4s.config.Config
import stryker4s.files.FileResolver
import stryker4s.model.{MutantResultsPerFile, RunResult}
import stryker4s.mutants.Mutator
import stryker4s.report.mapper.MutantRunResultMapper
import stryker4s.report.{FinishedRunEvent, Reporter}
import stryker4s.run.MutantRunner
import stryker4s.run.threshold.{ScoreStatus, ThresholdChecker}

class Stryker4s(fileSource: FileResolver, mutator: Mutator, runner: MutantRunner, reporter: Reporter)(implicit
    config: Config
) {

  def run(): IO[ScoreStatus] = for {
    (ignored, files) <- mutator.go(fileSource.files)
    result <- runner(files)
    metrics <- createAndReportResults(result, ignored)
    scoreStatus = ThresholdChecker.determineScoreStatus(metrics.mutationScore)
  } yield scoreStatus

  // TODO: move result reporting
  def createAndReportResults(results: RunResult, ignored: MutantResultsPerFile) = {
    val merged = results.results.alignCombine(ignored)
    val mapper = new MutantRunResultMapper() {}
    for {
      time <- IO.realTime
      report = mapper.toReport(merged, results.testFiles)
      metrics = Metrics.calculateMetrics(report)
      reportsLocation = config.baseDir / "target/stryker4s-report" / time.toMillis.toString()
      _ <- reporter.onRunFinished(FinishedRunEvent(report, metrics, results.duration, reportsLocation))
    } yield metrics
  }
}
