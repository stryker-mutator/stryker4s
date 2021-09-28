package stryker4s

import cats.effect.IO
import stryker4s.config.Config
import stryker4s.files.MutatesFileResolver
import stryker4s.mutants.Mutator
import stryker4s.run.MutantRunner
import stryker4s.run.threshold.{ScoreStatus, ThresholdChecker}

class Stryker4s(fileSource: MutatesFileResolver, mutator: Mutator, runner: MutantRunner)(implicit
    config: Config
) {

  def run(): IO[ScoreStatus] = {
    val filesToMutate = fileSource.files

    for {
      metrics <- runner(errors => mutator.mutate(filesToMutate, errors))
      scoreStatus = ThresholdChecker.determineScoreStatus(metrics.mutationScore)
    } yield scoreStatus
  }
}
