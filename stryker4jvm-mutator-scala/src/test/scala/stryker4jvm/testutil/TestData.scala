package stryker4jvm.testutil

import fs2.io.file.Path
import mutationtesting.*
import stryker4jvm.config.Config
import stryker4jvm.extensions.mutationtype.GreaterThan
import stryker4jvm.model.{MutantId, MutantMetadata, MutantWithId, MutatedCode}
import stryker4jvm.reporting.FinishedRunEvent

import scala.concurrent.duration.*
import scala.meta.quasiquotes.*

trait TestData {
  def createMutant =
    MutantWithId(MutantId(0), MutatedCode(q"<", MutantMetadata(">", "<", GreaterThan.mutationName, createLocation)))

  def createLocation = Location(Position(0, 0), Position(0, 0))

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
