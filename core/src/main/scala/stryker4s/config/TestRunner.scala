package stryker4s.config

sealed trait TestRunner

case class CommandRunner(command: String, args: String) extends TestRunner
