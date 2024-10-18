package stryker4s.model

import cats.syntax.option.*
import stryker4s.testrunner.api.{CoverageReport, TestFile}

import scala.concurrent.duration.FiniteDuration

sealed trait InitialTestRunResult {
  def isSuccessful: Boolean

  /** Initial testruns can report their own taken duration, or a timed one will be taken as a backup if this is `
    */
  def reportedDuration: Option[FiniteDuration] = none

  def coveredMutants: Map[MutantId, Seq[TestFile]]

  def staticMutants: Seq[MutantId]

  def hasCoverage: Boolean

  def testNames: Seq[TestFile]
}

final case class InitialTestRunCoverageReport(
    isSuccessful: Boolean,
    firstRun: CoverageReport,
    secondRun: CoverageReport,
    duration: FiniteDuration,
    testNames: Seq[TestFile]
) extends InitialTestRunResult {

  override val staticMutants: Seq[MutantId] = (firstRun.report -- (secondRun.report.keys)).keys.toSeq

  override val coveredMutants: Map[MutantId, Seq[TestFile]] = firstRun.report.filterNot { case (id, _) =>
    staticMutants.contains(id)
  }

  override def reportedDuration: Option[FiniteDuration] = duration.some

  override def hasCoverage: Boolean = true
}

final case class NoCoverageInitialTestRun(isSuccessful: Boolean) extends InitialTestRunResult {

  override def staticMutants: Seq[MutantId] = Seq.empty

  override def coveredMutants: Map[MutantId, Seq[TestFile]] = Map.empty

  override def hasCoverage: Boolean = false

  override def testNames: Seq[TestFile] = Seq.empty
}
