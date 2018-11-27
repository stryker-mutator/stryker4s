package stryker4s.run

import grizzled.slf4j.Logging
import stryker4s.config.Config
import stryker4s.model.MutantRunResults

class ThresholdChecker extends Logging {

  def determineExitCode(runResults: MutantRunResults)(implicit config: Config): Int = {
    config.thresholds match {
      case None =>
        debug("Threshold not configured. Won't fail the build no matter how low your mutation score is.")
        debug("Set `thresholds.break` to change this behavior.")
        0
      case Some(thresholds) if runResults.mutationScore < thresholds.break =>
        error(s"Mutation score below threshold! Score: ${runResults.mutationScore}. Threshold: ${thresholds.break}")
        1
      case Some(thresholds) if runResults.mutationScore >= thresholds.break =>
        info(s"Mutation score ${runResults.mutationScore} was above or equal to the configured threshold. ")
        0
    }
  }
}
