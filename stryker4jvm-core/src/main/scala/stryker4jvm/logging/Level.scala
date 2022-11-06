package stryker4jvm.logging

sealed trait Level

case object Debug extends Level
case object Info extends Level
case object Warn extends Level
case object Error extends Level
