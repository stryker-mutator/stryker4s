package stryker4s.command.runner

import java.nio.file.Path

import better.files.File
import stryker4s.config.Config
import stryker4s.model._
import stryker4s.mutants.findmutants.SourceCollector
import stryker4s.report.Reporter
import stryker4s.run.MutantRunner
import stryker4s.run.process.{Command, ProcessRunner}

import scala.concurrent.TimeoutException
import scala.util.{Failure, Success}

class ProcessMutantRunner(
    command: Command,
    processRunner: ProcessRunner,
    sourceCollector: SourceCollector,
    reporter: Reporter
)(implicit config: Config)
    extends MutantRunner(sourceCollector, reporter) {
  type Context = CommandRunnerContext

  override def runMutant(mutant: Mutant, context: Context): Path => MutantRunResult = {
    val id = mutant.id
    processRunner(command, context.tmpDir, ("ACTIVE_MUTATION", id.toString)) match {
      case Success(0)                         => Survived(mutant, _)
      case Success(exitCode) if exitCode != 0 => Killed(mutant, _)
      case Failure(_: TimeoutException)       => TimedOut(mutant, _)
      case _                                  => Error(mutant, _)
    }
  }

  override def runInitialTest(context: Context): Boolean = {
    processRunner(command, context.tmpDir, ("ACTIVE_MUTATION", "None")) match {
      case Success(0)                         => true
      case Success(exitCode) if exitCode != 0 => false
      case Failure(_: TimeoutException)       => false
    }
  }

  override def initializeTestContext(workingDir: File): Context = CommandRunnerContext(workingDir)

}
