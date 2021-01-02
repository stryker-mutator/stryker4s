package stryker4s.run.process

import scala.sys.process.{Process, ProcessLogger}
import scala.util.Try

import better.files.File
import cats.effect.{Blocker, ContextShift, IO}
import stryker4s.log.Logger

abstract class ProcessRunner(implicit log: Logger, cs: ContextShift[IO]) {
  def apply(command: Command, workingDir: File): Try[Seq[String]] = {
    Try {
      Process(s"${command.command} ${command.args}", workingDir.toJava)
        .!!<(ProcessLogger(log.debug(_)))
        .linesIterator
        .toSeq
    }
  }

  def apply(command: Command, workingDir: File, envVar: (String, String), blocker: Blocker): IO[Try[Int]] = {
    ProcessResource
      .fromProcessBuilder(
        Process(s"${command.command} ${command.args}", workingDir.toJava, envVar)
      )(m => log.debug(s"testrunner: $m"))
      .use(p => blocker.delay[IO, Int](p.exitValue()))
      .attempt
      .map(_.toTry)
  }
}

object ProcessRunner {
  private def isWindows: Boolean = sys.props("os.name").toLowerCase.contains("windows")

  def apply()(implicit log: Logger, cs: ContextShift[IO]): ProcessRunner = {
    if (isWindows) new WindowsProcessRunner
    else new UnixProcessRunner
  }
}
