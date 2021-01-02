package stryker4s.run.process

import scala.util.Try

import better.files.File
import cats.effect.{Blocker, ContextShift, IO}
import stryker4s.log.Logger

class WindowsProcessRunner(implicit log: Logger, cs: ContextShift[IO]) extends ProcessRunner {
  override def apply(command: Command, workingDir: File): Try[Seq[String]] = {
    super.apply(Command(s"cmd /c ${command.command}", command.args), workingDir)
  }

  override def apply(command: Command, workingDir: File, envVar: (String, String), blocker: Blocker): IO[Try[Int]] = {
    super.apply(Command(s"cmd /c ${command.command}", command.args), workingDir, envVar, blocker)
  }
}
