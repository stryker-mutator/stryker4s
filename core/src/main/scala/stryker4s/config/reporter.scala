package stryker4s.config

sealed trait Reporter

final case class ConsoleReporter(logSurvived: Boolean = false) extends Reporter