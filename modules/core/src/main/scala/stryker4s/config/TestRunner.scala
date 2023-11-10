package stryker4s.config

sealed trait TestRunner

final case class CommandRunner(command: String, args: String) extends TestRunner
