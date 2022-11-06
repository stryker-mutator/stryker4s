package stryker4jvm.reporting

import cats.effect.IO
import cats.syntax.applicative.*
import fs2.io.file.Path
import fs2.Pipe
import mutationtesting.*
import stryker4jvm.config.Config

import scala.concurrent.duration.FiniteDuration

abstract class Reporter {
  def mutantTested: Pipe[IO, MutantTestedEvent, Nothing] = in => in.drain
  def onRunFinished(runReport: FinishedRunEvent): IO[Unit] = runReport.pure[IO].void
}

final case class MutantTestedEvent(totalMutants: Int) extends AnyVal

final case class FinishedRunEvent(
    report: MutationTestResult[Config],
    metrics: MetricsResult,
    duration: FiniteDuration,
    reportsLocation: Path
)
