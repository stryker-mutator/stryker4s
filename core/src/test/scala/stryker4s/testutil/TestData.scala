package stryker4s.testutil

import fs2.io.file.Path
import mutationtesting.{Metrics, MetricsResult, MutationTestResult, Thresholds}
import stryker4s.config.Config
import stryker4s.extension.mutationtype.GreaterThan
import stryker4s.model.{Mutant, MutantId}
import stryker4s.report.FinishedRunEvent

import scala.concurrent.duration.*
import scala.meta.*

trait TestData {
  def createMutant = Mutant(MutantId(0), q"<", q">", GreaterThan)

  def createMutationTestResult = MutationTestResult(thresholds = Thresholds(100, 0), files = Map.empty)

  def createFinishedRunEvent(
      testResult: MutationTestResult[Config] = createMutationTestResult,
      metrics: Option[MetricsResult] = None
  ) =
    FinishedRunEvent(
      testResult,
      metrics.getOrElse(Metrics.calculateMetrics(testResult)),
      10.seconds,
      Path("target/stryker4s-report/")
    )
}
