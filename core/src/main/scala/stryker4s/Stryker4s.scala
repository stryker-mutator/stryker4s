package stryker4s

import stryker4s.config.Config
import stryker4s.mutants.Mutator
import stryker4s.mutants.findmutants.SourceCollector
import stryker4s.run.MutantRunner
import stryker4s.run.threshold.{ScoreStatus, ThresholdChecker}

class Stryker4s(fileCollector: SourceCollector, mutator: Mutator, runner: MutantRunner)(implicit config: Config) {

  def run(): ScoreStatus = {
    val filesToMutate = fileCollector.collectFilesToMutate()
    val mutatedFiles = mutator.mutate(filesToMutate)
    val metrics = runner(mutatedFiles).unsafeRunSync() // TODO: Don't use unsafeRunSync()
    ThresholdChecker.determineScoreStatus(metrics.mutationScore)
  }

}
