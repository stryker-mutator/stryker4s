package stryker4s.model

import better.files.File
import sbt.Tests
import sbt.testing.Framework
import stryker4s.sbt.runner.ProcessManager

final case class SbtRunnerContext(
    frameworks: Seq[Framework],
    testGroups: Seq[Tests.Group],
    processHandler: ProcessManager,
    tmpDir: File
) extends TestRunnerContext
