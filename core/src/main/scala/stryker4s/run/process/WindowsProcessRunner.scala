package stryker4s.run.process

import cats.effect.IO
import fs2.io.file.Path
import stryker4s.config.Config
import stryker4s.log.Logger

import scala.util.Try

class WindowsProcessRunner(implicit log: Logger) extends ProcessRunner {
  override def apply(command: Command, workingDir: Path): Try[Seq[String]] =
    super.apply(Command(s"cmd /c ${command.command}", command.args), workingDir)

  override def apply(command: Command, workingDir: Path, envVar: (String, String)*)(implicit
      config: Config
  ): IO[Try[Int]] = super.apply(Command(s"cmd /c ${command.command}", command.args), workingDir, envVar: _*)

}
