package stryker4jvm.run.process

import cats.effect.IO
import fs2.io.file.Path
import stryker4jvm.config.Config
import stryker4jvm.core.logging.Logger

import scala.util.Try

class WindowsProcessRunner(implicit log: Logger) extends ProcessRunner {
  override def apply(command: Command, workingDir: Path): Try[Seq[String]] =
    super.apply(Command(s"cmd /c ${command.command}", command.args), workingDir)

  override def apply(command: Command, workingDir: Path, envVar: (String, String)*)(implicit
      config: Config
  ): IO[Try[Int]] = super.apply(Command(s"cmd /c ${command.command}", command.args), workingDir, envVar*)

}
