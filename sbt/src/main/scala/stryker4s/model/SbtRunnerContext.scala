package stryker4s.model

import better.files.File
import sbt.Tests
import sbt.testing.Framework
import stryker4s.run.TestRunner

final case class SbtRunnerContext(
    frameworks: Seq[Framework],
    testGroups: Seq[Tests.Group],
    testRunner: TestRunner,
    tmpDir: File
) extends TestRunnerContext
