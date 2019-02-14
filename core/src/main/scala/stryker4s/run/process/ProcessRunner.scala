package stryker4s.run.process

import better.files.File
import grizzled.slf4j.Logging

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.{Duration, MINUTES}
import scala.concurrent.{Await, Future}
import scala.sys.process.{Process, ProcessLogger}
import scala.util.Try

trait ProcessRunner extends Logging {

  def apply(command: Command, workingDir: File): Try[Seq[String]] = {
    Try {
      Process(s"${command.command} ${command.args}", workingDir.toJava)
        .lineStream(ProcessLogger(debug(_)))
    }
  }

  def apply(command: Command, workingDir: File, envVar: (String, String)): Try[Int] = {
    val mutantProcess = Process(s"${command.command} ${command.args}", workingDir.toJava, envVar)
      .run(ProcessLogger(debug(_)))

    val exitCodeFuture = Future(mutantProcess.exitValue())
    // TODO: Maybe don't use Await.result
    // TODO: Use timeout decided by initial test-run duration
    Try(Await.result(exitCodeFuture, Duration(2, MINUTES)))
  }
}

object ProcessRunner {
  private val isWindows: Boolean = sys.props("os.name").toLowerCase.contains("windows")

  def apply(): ProcessRunner = {
    if (isWindows) new WindowsProcessRunner
    else new UnixProcessRunner
  }
}
