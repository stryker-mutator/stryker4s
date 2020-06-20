package stryker4s.sbt.testrunner

import stryker4s.api.testprocess.TestProcessConfig

object Context {
  def resolveSocketConfig(args: Array[String]): TestProcessConfig = {
    TestProcessConfig
      .fromArgs(args.toSeq)
      .getOrElse(throw new Exception(s"Could not resolve config arguments ${args.mkString(" ")}"))
  }

}
