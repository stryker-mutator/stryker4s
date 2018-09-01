package stryker4s.run.process

import better.files.File

import scala.util.Try

class WindowsProcessRunner extends ProcessRunner {
  override def apply(command: Command,
                     workingDir: File,
                     envVar: (String, String)): Try[Int] = {
    super.apply(Command("cmd /c " + command.command, command.args),
                workingDir,
                envVar)
  }
}
