package stryker4s.model

import better.files.File

trait TestRunnerContext {
  val tmpDir: File
}
