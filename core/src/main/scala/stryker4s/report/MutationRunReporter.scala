package stryker4s.report

import better.files.File
import cats.effect.IO
import fs2.{INothing, Pipe}
import mutationtesting._
import stryker4s.config.Config
import stryker4s.model.{Mutant, MutantRunResult}

import java.nio.file.Path
import scala.concurrent.duration.FiniteDuration

trait Reporter {
  def mutantPlaced: Pipe[IO, Mutant, INothing] = in => in.drain
  def mutantTested: Pipe[IO, (Path, MutantRunResult), INothing] = in => in.drain
  def onRunFinished(runReport: FinishedRunEvent): IO[Unit] = IO.unit
}

case class StartMutationEvent(progress: Progress)

final case class Progress(tested: Int, total: Int)

final case class FinishedRunEvent(
    report: MutationTestResult[Config],
    metrics: MetricsResult,
    duration: FiniteDuration,
    reportsLocation: File
)
