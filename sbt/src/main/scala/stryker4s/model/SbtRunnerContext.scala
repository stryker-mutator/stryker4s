package stryker4s.model

import stryker4s.run.TestRunner

final case class SbtRunnerContext(testRunner: TestRunner) extends TestRunnerContext
