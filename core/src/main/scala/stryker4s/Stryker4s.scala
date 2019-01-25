package stryker4s

import grizzled.slf4j.Logging
import stryker4s.config.Config
import stryker4s.mutants.Mutator
import stryker4s.mutants.findmutants.SourceCollector
import stryker4s.run.MutantRunner
import stryker4s.run.report.Reporter
import stryker4s.run.threshold.{ScoreStatus, ThresholdChecker}

class Stryker4s(fileCollector: SourceCollector, mutator: Mutator, runner: MutantRunner, reporter: Reporter)(
    implicit config: Config) extends Logging {

  def run(): ScoreStatus = {
    preRunLogs()
    val filesToMutate = fileCollector.collectFilesToMutate()
    val mutatedFiles = mutator.mutate(filesToMutate)
    val runResults = runner(mutatedFiles, fileCollector)
    reporter.report(runResults)
    ThresholdChecker.determineScoreStatus(runResults.mutationScore)
  }

  private def preRunLogs(): Unit = {
    // If JVM max memory is less than 2GB, log warning
    if (!jvmHasEnoughMemory) {
      warn("The JVM doesn't have a lot of memory assigned to it. It's wise to increase the maximum memory when running stryker.")
      warn("This can be done in SBT by adding setting an environment variable: SBT_OPTS=\"-XX:+CMSClassUnloadingEnabled -XX:MaxPermSize=4G -Xmx4G\" ")
    }
  }
  protected def jvmHasEnoughMemory: Boolean =
    (Runtime.getRuntime.maxMemory / 1024 / 1024) >= 1820
}
