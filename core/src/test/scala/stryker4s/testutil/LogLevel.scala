package stryker4s.testutil

sealed trait LogLevel
object LogLevel {
  case object Error extends LogLevel
  case object Warn extends LogLevel
  case object Info extends LogLevel
  case object Debug extends LogLevel
}
