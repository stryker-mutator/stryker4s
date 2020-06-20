package stryker4s.model

import better.files.File
import stryker4s.sbt.runner.ProcessManager
import sbt.Tests
import sbt.testing.Framework

final case class SbtRunnerContext(
    frameworks: Seq[Framework],
    testGroups: Seq[Tests.Group],
    processHandler: ProcessManager,
    tmpDir: File
) extends TestRunnerContext
