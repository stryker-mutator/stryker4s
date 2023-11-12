package stryker4s.model

import cats.syntax.option.*
import stryker4s.testrunner.api.testprocess.CoverageReport

import scala.concurrent.duration.FiniteDuration

sealed trait InitialTestRunResult {
  def isSuccessful: Boolean

  /** Initial testruns can report their own taken duration, or a timed one will be taken as a backup if this is `
    */
  def reportedDuration: Option[FiniteDuration] = none
}

final case class InitialTestRunCoverageReport(
    isSuccessful: Boolean,
    firstRun: CoverageReport,
    secondRun: CoverageReport,
    duration: FiniteDuration
) extends InitialTestRunResult {
  override def reportedDuration: Option[FiniteDuration] = duration.some
}

final case class NoCoverageInitialTestRun(isSuccessful: Boolean) extends InitialTestRunResult
