package stryker4s.command.runner

import scala.concurrent.TimeoutException
import scala.util.{Failure, Success}

import better.files.File
import cats.effect.{Blocker, IO}
import stryker4s.model._
import stryker4s.run.process.{Command, ProcessRunner}
import stryker4s.run.{InitialTestRunResult, TestRunner}

class ProcessTestRunner(command: Command, processRunner: ProcessRunner, tmpDir: File, blocker: Blocker)
    extends TestRunner {
  def initialTestRun(): IO[InitialTestRunResult] = {
    processRunner(command, tmpDir, ("ACTIVE_MUTATION", "None"), blocker).map {
      case Success(0) => Left(true)
      case _          => Left(false)
    }
  }

  def runMutant(mutant: Mutant): IO[MutantRunResult] = {
    val id = mutant.id
    processRunner(command, tmpDir, ("ACTIVE_MUTATION", id.toString), blocker).map {
      case Success(0)                   => Survived(mutant)
      case Success(_)                   => Killed(mutant)
      case Failure(_: TimeoutException) => TimedOut(mutant)
      case _                            => Error(mutant)
    }
  }

}
