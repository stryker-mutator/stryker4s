package stryker4s.model

import better.files.File

final case class CommandRunnerContext(tmpDir: File) extends TestRunnerContext
