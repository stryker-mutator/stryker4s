package stryker4s.command.runner

import cats.effect.IO
import fs2.io.file.Path
import stryker4s.config.Config
import stryker4s.model._
import stryker4s.run.TestRunner
import stryker4s.run.process.{Command, ProcessRunner}

import scala.concurrent.TimeoutException
import scala.util.{Failure, Success}
import stryker4s.api.testprocess.Fingerprint

class ProcessTestRunner(command: Command, processRunner: ProcessRunner, tmpDir: Path)(implicit config: Config)
    extends TestRunner {
  def initialTestRun(): IO[InitialTestRunResult] = {
    processRunner(command, tmpDir, List.empty[(String, String)]: _*).map {
      case Success(0) => NoCoverageInitialTestRun(true)
      case _          => NoCoverageInitialTestRun(false)
    }
  }

  def runMutant(mutant: Mutant, fingerprints: Seq[Fingerprint]): IO[MutantRunResult] = {
    val id = mutant.id.globalId
    processRunner(command, tmpDir, ("ACTIVE_MUTATION", id.toString)).map {
      case Success(0)                   => Survived(mutant)
      case Success(_)                   => Killed(mutant)
      case Failure(_: TimeoutException) => TimedOut(mutant)
      case _                            => Error(mutant)
    }
  }

}
