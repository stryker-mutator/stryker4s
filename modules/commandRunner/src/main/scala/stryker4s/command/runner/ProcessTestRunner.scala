package stryker4s.command.runner

import cats.effect.IO
import fs2.io.file.Path
import mutationtesting.{MutantResult, MutantStatus}
import stryker4s.config.Config
import stryker4s.model.*
import stryker4s.run.TestRunner
import stryker4s.run.process.{Command, ProcessRunner}
import stryker4s.testrunner.api.TestFile

import scala.concurrent.TimeoutException
import scala.util.{Failure, Success}

class ProcessTestRunner(command: Command, processRunner: ProcessRunner, tmpDir: Path)(implicit config: Config)
    extends TestRunner {
  def initialTestRun(): IO[InitialTestRunResult] = {
    processRunner(command, tmpDir, List.empty[(String, String)]*).map {
      case Success(0) => NoCoverageInitialTestRun(true)
      case _          => NoCoverageInitialTestRun(false)
    }
  }

  def runMutant(mutant: MutantWithId, testNames: Seq[TestFile]): IO[MutantResult] = {
    val id = mutant.id.value
    processRunner(command, tmpDir, ("ACTIVE_MUTATION", id.toString)).map {
      case Success(0)                   => mutant.toMutantResult(MutantStatus.Survived)
      case Success(_)                   => mutant.toMutantResult(MutantStatus.Killed)
      case Failure(_: TimeoutException) => mutant.toMutantResult(MutantStatus.Timeout)
      case _                            => mutant.toMutantResult(MutantStatus.RuntimeError)
    }
  }

}
