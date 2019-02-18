package stryker4s

import grizzled.slf4j.Logging
import stryker4s.config.Config
import stryker4s.mutants.Mutator
import stryker4s.mutants.findmutants.SourceCollector
import stryker4s.run.MutantRunner
import stryker4s.run.report.Reporter
import stryker4s.run.threshold.{ScoreStatus, ThresholdChecker}

class Stryker4s(fileCollector: SourceCollector, mutator: Mutator, runner: MutantRunner, reporter: Reporter)(
    implicit config: Config)
    extends Logging {

  def run(): ScoreStatus = {
    preRunLogs()

    val filesToMutate = fileCollector.collectFilesToMutate()
    val mutatedFiles = mutator.mutate(filesToMutate)
    val runResults = runner(mutatedFiles)
    reporter.report(runResults)
    ThresholdChecker.determineScoreStatus(runResults.mutationScore)
  }

  private def preRunLogs(): Unit = {
    // sbt grants 1 GB to the JVM by default. We log a warning when the JVM has less than 2 GB,
    // to encourage the user to make a decision here.
    if (!jvmMemory2GBOrHigher) {
      warn("The JVM has less than 2GB memory available. We advise increasing this to 4GB when running Stryker4s.")
      warn(
        "This can be done in sbt by setting an environment variable: SBT_OPTS=\"-XX:+CMSClassUnloadingEnabled -XX:MaxPermSize=4G -Xmx4G\" ")
      warn("Visit https://github.com/stryker-mutator/stryker4s#memory-usage for more info.")
    }
  }
  protected def jvmMemory2GBOrHigher: Boolean =
    // Setting SBT_OPTS to 2GB gives 1820 MB here
    (Runtime.getRuntime.maxMemory / 1024 / 1024) >= 1820
}
