package stryker4s

import cats.effect.IO
import mutationtesting.MetricsResult
import stryker4s.config.Config
import stryker4s.files.MutatesFileResolver
import stryker4s.mutants.Mutator
import stryker4s.run.MutantRunner
import stryker4s.run.threshold.{ScoreStatus, ThresholdChecker}

case class CompileError(msg: String, path: String, line: Integer) {
  override def toString: String = s"$path:L$line: '$msg'"
}
case class MutationCompilationFailed(errors: Seq[CompileError]) extends RuntimeException(s"Compilation failed: $errors")

class Stryker4s(fileSource: MutatesFileResolver, mutatorFactory: () => Mutator, runner: MutantRunner)(implicit
    config: Config
) {

  def run(): IO[ScoreStatus] = {
    runWithRetry(List.empty)
      .map(metrics => ThresholdChecker.determineScoreStatus(metrics.mutationScore))
  }

  //Retries the run, after removing the non-compiling mutants
  def runWithRetry(compilerErrors: Seq[CompileError]): IO[MetricsResult] = {
    //Recreate the mutator from the factory, otherwise the second run's ids will not start at 0
    val mutator = mutatorFactory()
    val filesToMutate = fileSource.files
    for {
      mutatedFiles <- mutator.mutate(filesToMutate, compilerErrors)
      metrics <- runner(mutatedFiles).handleErrorWith {
        //If a compiler error occurs, retry once without the lines that gave an error
        case MutationCompilationFailed(errs) if compilerErrors.isEmpty =>
          runWithRetry(errs)
        case MutationCompilationFailed(_) =>
          IO.raiseError(new RuntimeException("Tried and failed to remove non-compiling mutants"))
        //Something else went wrong vOv
        case e => IO.raiseError(e)
      }
    } yield metrics
  }
}
