package stryker4s.run.threshold

import stryker4s.config.Config

object ThresholdChecker {

  def determineScoreStatus(mutationScore: Double)(implicit config: Config): ScoreStatus =
    mutationScore match {
      case score if score < config.thresholds.break => ErrorStatus
      case score if score < config.thresholds.low   => DangerStatus
      case score if score < config.thresholds.high  => WarningStatus
      case _                                        => SuccessStatus
    }
}
