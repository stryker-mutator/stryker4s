package stryker4s.testutil

import cats.syntax.option.*
import fs2.io.file.Path
import mutationtesting.*
import stryker4s.config.Config
import stryker4s.model.{MutantId, MutantMetadata, MutantWithId, MutatedCode}
import stryker4s.mutation.GreaterThan
import stryker4s.report.FinishedRunEvent

import scala.concurrent.duration.*
import scala.meta.Term

trait TestData {
  def createMutant =
    MutantWithId(
      MutantId(0),
      MutatedCode(Term.Name("<"), MutantMetadata(">", "<", GreaterThan.mutationName, createLocation, none))
    )

  def createLocation = Location(Position(0, 0), Position(0, 0))

  def createMutationTestResult = MutationTestResult(thresholds = Thresholds(100, 0), files = Map.empty)

  def createFinishedRunEvent(
      testResult: MutationTestResult[Config] = createMutationTestResult,
      metrics: Option[MetricsResult] = none
  ) =
    FinishedRunEvent(
      testResult,
      metrics.getOrElse(Metrics.calculateMetrics(testResult)),
      10.seconds,
      Path("target/stryker4s-report/")
    )
}
