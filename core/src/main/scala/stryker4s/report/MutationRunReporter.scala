package stryker4s.report

import scala.concurrent.duration.FiniteDuration

import better.files.File
import cats.effect.IO
import mutationtesting._

sealed trait MutationRunReporter

trait ProgressReporter extends MutationRunReporter {
  def reportMutationStart(event: StartMutationEvent): IO[Unit]
}

trait FinishedRunReporter extends MutationRunReporter {
  def reportRunFinished(runReport: FinishedRunReport): IO[Unit]
}

case class StartMutationEvent(progress: Progress)

final case class Progress(tested: Int, total: Int)

final case class FinishedRunReport(
    report: MutationTestReport,
    metrics: MetricsResult,
    duration: FiniteDuration,
    reportsLocation: File
)
