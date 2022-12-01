package stryker4jvm.mutator.scala.config

sealed trait TestRunner

final case class CommandRunner(command: String, args: String) extends TestRunner
