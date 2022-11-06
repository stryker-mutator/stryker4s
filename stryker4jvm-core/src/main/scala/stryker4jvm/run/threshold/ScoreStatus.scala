package stryker4jvm.run.threshold

sealed trait ScoreStatus

case object SuccessStatus extends ScoreStatus
case object WarningStatus extends ScoreStatus
case object DangerStatus extends ScoreStatus
case object ErrorStatus extends ScoreStatus
