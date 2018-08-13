package stryker4s.config

trait TestRunner

case class CommandRunner(command: String) extends TestRunner
