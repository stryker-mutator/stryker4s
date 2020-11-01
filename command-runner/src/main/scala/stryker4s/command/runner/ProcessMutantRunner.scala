package stryker4s.command.runner

import scala.concurrent.TimeoutException
import scala.util.{Failure, Success}

import better.files.File
import cats.effect.{ContextShift, IO, Resource, Timer}
import stryker4s.config.Config
import stryker4s.model._
import stryker4s.mutants.findmutants.SourceCollector
import stryker4s.report.Reporter
import stryker4s.run.MutantRunner
import stryker4s.run.process.{Command, ProcessRunner}
import stryker4s.log.Logger

class ProcessMutantRunner(
    command: Command,
    processRunner: ProcessRunner,
    sourceCollector: SourceCollector,
    reporter: Reporter
)(implicit config: Config, log: Logger, timer: Timer[IO], cs: ContextShift[IO])
    extends MutantRunner(sourceCollector, reporter) {
  type Context = CommandRunnerContext

  override def runMutant(mutant: Mutant, context: Context): IO[MutantRunResult] = {
    val id = mutant.id
    IO(processRunner(command, context.tmpDir, ("ACTIVE_MUTATION", id.toString))) map {
      case Success(0)                         => Survived(mutant)
      case Success(exitCode) if exitCode != 0 => Killed(mutant)
      case Failure(_: TimeoutException)       => TimedOut(mutant)
      case _                                  => Error(mutant)
    }
  }

  override def runInitialTest(context: Context): IO[Boolean] = {
    IO(processRunner(command, context.tmpDir, ("ACTIVE_MUTATION", "None"))) map {
      case Success(0)                         => true
      case Success(exitCode) if exitCode != 0 => false
      case Failure(_: TimeoutException)       => false
    }
  }

  override def initializeTestContext(tmpDir: File): Resource[IO, Context] =
    Resource.pure[IO, Context](CommandRunnerContext(tmpDir))

}
