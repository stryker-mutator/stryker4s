package stryker4jvm

import cats.effect.IO
import cats.syntax.align.*
import mutationtesting.{Metrics, MetricsResult}
import stryker4jvm.config.Config
import stryker4jvm.core.model.AST
import stryker4jvm.core.reporting.Reporter
import stryker4jvm.core.reporting.events.FinishedRunEvent
import stryker4jvm.core.run.threshold.{ScoreStatus, Thresholds}
import stryker4jvm.files.MutatesFileResolver
import stryker4jvm.model.{MutantResultsPerFile, RunResult}
import stryker4jvm.mutants.Mutator
import stryker4jvm.reporting.mapper.MutantRunResultMapper
import stryker4jvm.run.MutantRunner

class Stryker4jvm(fileSource: MutatesFileResolver, mutator: Mutator, runner: MutantRunner, reporter: Reporter[Config])(implicit
    config: Config
) {

  def run(): IO[ScoreStatus] = {
    val filesToMutate = fileSource.files
    for {
      (ignored, files) <- mutator.go(filesToMutate)
      result <- runner(files)
      metrics <- createAndReportResults(result, ignored)
      scoreStatus = ScoreStatus.Success.determineScoreStatus(new Thresholds(), metrics.mutationScore)
    } yield scoreStatus
  }

  // TODO: move
  def createAndReportResults(results: RunResult, ignored: MutantResultsPerFile): IO[MetricsResult] = {
    val merged = results.results.alignCombine(ignored)
    val mapper = new MutantRunResultMapper() {}
    for {
      time <- IO.realTime
      report = mapper.toReport(merged)
      metrics = Metrics.calculateMetrics(report)
      reportsLocation = config.baseDir / "target/stryker4jvm-report" / time.toMillis.toString
      _ <- reporter.onRunFinished(FinishedRunEvent[AST](report, metrics, results.duration, reportsLocation))
    } yield metrics
  }
}
