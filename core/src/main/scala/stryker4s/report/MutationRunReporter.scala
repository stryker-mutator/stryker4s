package stryker4s.report

import scala.concurrent.duration.FiniteDuration

import better.files.File
import cats.effect.IO
import mutationtesting._
import stryker4s.config.Config

sealed trait MutationRunReporter

trait ProgressReporter extends MutationRunReporter {
  def onMutationStart(event: StartMutationEvent): IO[Unit]
}

trait FinishedRunReporter extends MutationRunReporter {
  def onRunFinished(runReport: FinishedRunEvent): IO[Unit]
}

case class StartMutationEvent(progress: Progress)

final case class Progress(tested: Int, total: Int)

final case class FinishedRunEvent(
    report: MutationTestResult[Config],
    metrics: MetricsResult,
    duration: FiniteDuration,
    reportsLocation: File
)
