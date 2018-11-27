package stryker4s.run

import grizzled.slf4j.Logging
import stryker4s.config.Config
import stryker4s.model.MutantRunResults

class ThresholdChecker extends Logging {

  def determineExitCode(runResults: MutantRunResults)(implicit config: Config): Int = {
    config.thresholds.break match {
      case 0 =>
        debug("Threshold configured at 0. Won\'t fail the build no matter how low your mutation score is.")
        debug("Set `thresholds.break` to a value higher than 0 to change this behavior.")
        0
      case threshold if runResults.mutationScore < threshold =>
        error(s"Mutation score below threshold! Score: ${runResults.mutationScore}. Threshold: $threshold")
        1
      case threshold if runResults.mutationScore >= threshold =>
        info(s"Mutation score ${runResults.mutationScore} was above or equal to the configured threshold. ")
        0
    }
  }
}
