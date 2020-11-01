package stryker4s.run.process

import scala.concurrent.duration.{Duration, MINUTES}
import scala.sys.process.{Process, ProcessLogger}
import scala.util.Try

import better.files.File
import cats.effect.IO
import stryker4s.log.Logger

abstract class ProcessRunner(implicit log: Logger) {
  def apply(command: Command, workingDir: File): Try[Seq[String]] = {
    Try {
      Process(s"${command.command} ${command.args}", workingDir.toJava)
        .!!<(ProcessLogger(log.debug(_)))
        .linesIterator
        .toSeq
    }
  }

  def apply(command: Command, workingDir: File, envVar: (String, String)): Try[Int] = {
    val mutantProcess = Process(s"${command.command} ${command.args}", workingDir.toJava, envVar)
      .run(ProcessLogger(log.debug(_)))

    val exitCodeFuture = IO(mutantProcess.exitValue())
    // TODO: Maybe don't use unsafeRunTimed
    // TODO: Use timeout decided by initial test-run duration
    Try(exitCodeFuture.unsafeRunTimed(Duration(2, MINUTES)).get)
  }
}

object ProcessRunner {
  private def isWindows: Boolean = sys.props("os.name").toLowerCase.contains("windows")

  def apply()(implicit log: Logger): ProcessRunner = {
    if (isWindows) new WindowsProcessRunner
    else new UnixProcessRunner
  }
}
