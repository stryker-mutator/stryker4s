package stryker4jvm.command.runner

import cats.effect.IO
import fs2.io.file.Path
import mutationtesting.{MutantResult, MutantStatus}
import stryker4jvm.config.Config
import stryker4jvm.core.model.{AST, MutantWithId}
import stryker4jvm.extensions.MutantExtensions.ToMutantResultExtension
import stryker4jvm.model.*
import stryker4jvm.run.TestRunner
import stryker4jvm.run.process.{Command, ProcessRunner}

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

  def runMutant(mutant: MutantWithId[AST], testNames: Seq[String]): IO[MutantResult] = {
    val id = mutant.id
    processRunner(command, tmpDir, ("ACTIVE_MUTATION", id.toString)).map {
      case Success(0)                   => mutant.toMutantResult(MutantStatus.Survived)
      case Success(_)                   => mutant.toMutantResult(MutantStatus.Killed)
      case Failure(_: TimeoutException) => mutant.toMutantResult(MutantStatus.Timeout)
      case _                            => mutant.toMutantResult(MutantStatus.RuntimeError)
    }
  }

}
