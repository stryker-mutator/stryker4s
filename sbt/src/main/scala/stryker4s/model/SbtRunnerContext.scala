package stryker4s.model

import better.files.File
import stryker4s.sbt.runner.ProcessHandler
import sbt.Tests

final case class SbtRunnerContext(testGroups: Seq[Tests.Group], processHandler: ProcessHandler, tmpDir: File)
    extends TestRunnerContext
