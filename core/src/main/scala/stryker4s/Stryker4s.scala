package stryker4s

import grizzled.slf4j.Logging
import stryker4s.config.Config
import stryker4s.mutants.Mutator
import stryker4s.mutants.findmutants.SourceCollector
import stryker4s.run.MutantRunner
import stryker4s.run.threshold.{ScoreStatus, ThresholdChecker}

class Stryker4s(fileCollector: SourceCollector, mutator: Mutator, runner: MutantRunner)(implicit config: Config)
    extends Logging {

  //The minimal memory for sbt that is recommended for Stryker4s to run smoothly
  private[this] val minimalMemoryRecommendation = 1820 * 1024 * 1024

  def run(): ScoreStatus = {
    validateAllocatedMemory()
    val filesToMutate = fileCollector.collectFilesToMutate()
    val mutatedFiles = mutator.mutate(filesToMutate)
    val runResults = runner(mutatedFiles)
    ThresholdChecker.determineScoreStatus(runResults.mutationScore)
  }

  private def validateAllocatedMemory(): Unit = {
    // sbt grants 1 GB to the JVM by default. We log a warning when the JVM has less than 2 GB,
    // to encourage the user to make a decision here.
    if (!jvmMemory2GBOrHigher) {
      warn("The JVM has less than 2GB memory available. We advise to allocate 4GB memory when running Stryker4s.")
      warn(
        "Visit https://github.com/stryker-mutator/stryker4s#memory-usage for more info on how to allocate more memory to the JVM.")
    }
  }

  protected def jvmMemory2GBOrHigher: Boolean =
    // Setting SBT_OPTS to 2GB gives 1820 MB here
    Runtime.getRuntime.maxMemory >= minimalMemoryRecommendation
}
