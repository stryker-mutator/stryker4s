package stryker4s

import stryker4s.config.Config
import stryker4s.mutants.Mutator
import stryker4s.mutants.findmutants.SourceCollector
import stryker4s.run.MutantRunner
import stryker4s.run.threshold.{ScoreStatus, ThresholdChecker}
import cats.effect.IO

class Stryker4s(fileCollector: SourceCollector, mutator: Mutator, runner: MutantRunner)(implicit config: Config) {

  def run(): IO[ScoreStatus] =
    for {
      filesToMutate <- IO(fileCollector.collectFilesToMutate())
      mutatedFiles = mutator.mutate(filesToMutate).toList
      metrics <- runner(mutatedFiles)
      scoreStatus = ThresholdChecker.determineScoreStatus(metrics.mutationScore)
    } yield scoreStatus

}
