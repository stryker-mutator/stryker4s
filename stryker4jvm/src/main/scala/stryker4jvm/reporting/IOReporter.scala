package stryker4jvm.reporting

import cats.effect.IO
import fs2.Pipe
import stryker4jvm.core.reporting.events.{FinishedRunEvent, MutantTestedEvent}
import cats.syntax.applicative.*
import stryker4jvm.core.reporting.Reporter

import scala.language.implicitConversions

class IOReporter[C] {
  def mutantTested: Pipe[IO, MutantTestedEvent, Nothing] = in => in.drain
  def onRunFinished(runReport: FinishedRunEvent[C]): IO[Unit] = runReport.pure[IO].void
}

class ReporterWrapper[C](val reporter: Reporter[C]) extends IOReporter[C] {
  override def mutantTested: Pipe[IO, MutantTestedEvent, Nothing] = in => in.map(reporter.mutantTested).drain
  override def onRunFinished(runReport: FinishedRunEvent[C]): IO[Unit] = IO(reporter.onRunFinished(runReport))
}