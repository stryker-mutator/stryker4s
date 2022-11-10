package stryker4jvm.run.process

import cats.effect.IO
import fs2.io.file.Path
import stryker4jvm.config.Config
import stryker4jvm.logging.Logger

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

  def apply(command: Command, workingDir: Path, envVar: (String, String)*)(implicit config: Config): IO[Try[Int]] = {
    val logger: String => Unit =
      if (config.debug.logTestRunnerStdout) m => log.debug(s"testrunner: $m")
      else _ => ()

    ProcessResource
      .fromProcessBuilder(
        Process(s"${command.command} ${command.args}", workingDir.toNioPath.toFile(), envVar*)
      )(logger)
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
