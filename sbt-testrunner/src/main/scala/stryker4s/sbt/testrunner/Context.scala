package stryker4s.sbt.testrunner

import sbt.testing.TaskDef

object Context {
  def resolveContext(args: Array[String]): Context = ???
}

final case class Context(
    frameworkClass: String,
    taskDefs: Array[TaskDef],
    runnerOptions: RunnerOptions
)

final case class RunnerOptions(args: Array[String], remoteArgs: Array[String])
