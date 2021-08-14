package stryker4s.run.process

import cats.effect.IO
import fs2.io.file.Path
import stryker4s.log.Logger

import scala.sys.process.{Process, ProcessLogger}
import scala.util.Try

abstract class ProcessRunner(implicit log: Logger) {
  def apply(command: Command, workingDir: Path): Try[Seq[String]] = {
    Try {
      Process(s"${command.command} ${command.args}", workingDir.toNioPath.toFile())
        .!!<(ProcessLogger(log.debug(_)))
        .linesIterator
        .toSeq
    }
  }

  def apply(command: Command, workingDir: Path, envVar: (String, String)*): IO[Try[Int]] = {
    ProcessResource
      .fromProcessBuilder(
        Process(s"${command.command} ${command.args}", workingDir.toNioPath.toFile(), envVar: _*)
      )(m => log.debug(s"testrunner: $m"))
      .use(p => IO.blocking(p.exitValue()))
      .attempt
      .map(_.toTry)
  }
}

object ProcessRunner {
  private def isWindows: Boolean = sys.props("os.name").toLowerCase.contains("windows")

  def apply()(implicit log: Logger): ProcessRunner = {
    if (isWindows) new WindowsProcessRunner
    else new UnixProcessRunner
  }
}
