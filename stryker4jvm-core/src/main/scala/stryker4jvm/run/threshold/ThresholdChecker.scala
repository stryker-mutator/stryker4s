package stryker4jvm.run.threshold

import stryker4jvm.config.Config

object ThresholdChecker {
  def determineScoreStatus(mutationScore: Double)(implicit config: Config): ScoreStatus =
    mutationScore match {
      case score if score < config.thresholds.break => ErrorStatus
      case score if score < config.thresholds.low   => DangerStatus
      case score if score < config.thresholds.high  => WarningStatus
      case _                                        => SuccessStatus
    }
}
