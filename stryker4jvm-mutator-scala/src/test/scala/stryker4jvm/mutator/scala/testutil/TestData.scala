package stryker4jvm.mutator.scala.testutil

import java.nio.file.{Path, Paths}
import mutationtesting.*
import stryker4jvm.mutator.scala.config.Config
import stryker4jvm.mutator.scala.extensions.mutationtype.GreaterThan
import stryker4jvm.core.model.{MutantMetaData, MutantWithId, MutatedCode}
import stryker4jvm.mutator.scala.extensions.Stryker4jvmCoreConversions.LocationExtension

import scala.concurrent.duration.*
import scala.meta.quasiquotes.*

trait TestData {
  def createMutant =
    new MutantWithId(
      0,
      new MutatedCode(q"<", new MutantMetaData(">", "<", GreaterThan.mutationName, createLocation.asCoreElement))
    )

  def createLocation = Location(Position(0, 0), Position(0, 0))

  def createMutationTestResult = MutationTestResult(thresholds = Thresholds(100, 0), files = Map.empty)

//  def createFinishedRunEvent(
//      testResult: MutationTestResult[Config] = createMutationTestResult,
//      metrics: Option[MetricsResult] = None
//  ) =
//    new FinishedRunEvent(
//      testResult,
//      metrics.getOrElse(Metrics.calculateMetrics(testResult)),
//      10.seconds,
//      Paths.get("target/stryker4s-report/")
//    )
}
