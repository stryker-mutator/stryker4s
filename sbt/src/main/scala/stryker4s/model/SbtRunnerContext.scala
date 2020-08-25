package stryker4s.model

import better.files.File
import stryker4s.run.TestRunner

final case class SbtRunnerContext(
    testRunner: TestRunner,
    tmpDir: File
) extends TestRunnerContext
