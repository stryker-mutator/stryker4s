package stryker4s.run.threshold

import stryker4s.config.Config
import stryker4s.model.MutantRunResults

object ThresholdChecker {

  def determineExitCode(runResults: MutantRunResults)(implicit config: Config): Int = {
    getScoreStatus(runResults.mutationScore) match {
      case ErrorStatus => 1
      case _           => 0
    }
  }
  def getScoreStatus(mutationScore: Double)(implicit config: Config): ScoreStatus = {
    mutationScore match {
      case score if score < config.thresholds.break => ErrorStatus
      case score if score < config.thresholds.low   => DangerStatus
      case score if score < config.thresholds.high  => WarningStatus
      case _                                        => SuccessStatus
    }
  }
}
