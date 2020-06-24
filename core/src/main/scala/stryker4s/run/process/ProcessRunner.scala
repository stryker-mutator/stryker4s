package stryker4s.run.process

import better.files.File
import grizzled.slf4j.Logging

import scala.concurrent.duration.{Duration, MINUTES}
import scala.sys.process.{Process, ProcessLogger}
import scala.util.Try
import cats.effect.IO

trait ProcessRunner extends Logging {
  def apply(command: Command, workingDir: File): Try[Seq[String]] = {
    Try {
      Process(s"${command.command} ${command.args}", workingDir.toJava)
        .!!<(ProcessLogger(debug(_)))
        .linesIterator
        .toSeq
    }
  }

  def apply(command: Command, workingDir: File, envVar: (String, String)): Try[Int] = {
    val mutantProcess = Process(s"${command.command} ${command.args}", workingDir.toJava, envVar)
      .run(ProcessLogger(debug(_)))

    val exitCodeFuture = IO(mutantProcess.exitValue())
    // TODO: Maybe don't use unsafeRunTimed
    // TODO: Use timeout decided by initial test-run duration
    Try(exitCodeFuture.unsafeRunTimed(Duration(2, MINUTES)).get)
  }
}

object ProcessRunner {
  private def isWindows: Boolean = sys.props("os.name").toLowerCase.contains("windows")

  def apply(): ProcessRunner = {
    if (isWindows) new WindowsProcessRunner
    else new UnixProcessRunner
  }
}
