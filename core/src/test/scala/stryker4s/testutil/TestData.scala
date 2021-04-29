package stryker4s.testutil

import better.files.File
import mutationtesting.{Metrics, MetricsResult, MutationTestResult, Thresholds}
import stryker4s.config.Config
import stryker4s.extension.mutationtype.GreaterThan
import stryker4s.model.Mutant
import stryker4s.report.FinishedRunEvent

import scala.concurrent.duration._
import scala.meta._

trait TestData {
  def createMutant = Mutant(0, q"<", q">", GreaterThan)

  def createMutationTestResult = MutationTestResult(thresholds = Thresholds(100, 0), files = Map.empty)

  def createFinishedRunEvent(
      testResult: MutationTestResult[Config] = createMutationTestResult,
      metrics: Option[MetricsResult] = None
  ) =
    FinishedRunEvent(
      testResult,
      metrics.getOrElse(Metrics.calculateMetrics(testResult)),
      10.seconds,
      File("target/stryker4s-report/")
    )
}
