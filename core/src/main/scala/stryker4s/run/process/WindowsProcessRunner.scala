package stryker4s.run.process

import better.files.File

import scala.util.Try

class WindowsProcessRunner extends ProcessRunner {
  override def apply(command: String, workingDir: File, envVar: (String, String)): Try[Int] = {
    super.apply("cmd /c " + command, workingDir, envVar)
  }
}
