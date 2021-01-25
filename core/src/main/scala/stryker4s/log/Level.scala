package stryker4s.log

sealed trait Level

case object Debug extends Level
case object Info extends Level
case object Warn extends Level
case object Error extends Level
