package stryker4s.run.process

import better.files.File
import grizzled.slf4j.Logging

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.{Duration, MINUTES}
import scala.concurrent.{Await, Future}
import scala.sys.process.{Process, ProcessLogger}
import scala.util.Try

trait ProcessRunner extends Logging {
  def apply(command: Command, workingDir: File, envVar: (String, String)): Try[Int] = {
    val mutantProcess = Process(command.command + " " + command.args, workingDir.toJava, envVar)
      .run(ProcessLogger(s => debug(s)))

    val exitCodeFuture = Future(mutantProcess.exitValue())
    // TODO: Maybe don't use Await.result
    // TODO: Use timeout decided by initial test-run duration
    Try(Await.result(exitCodeFuture, Duration(2, MINUTES)))
  }
}

object ProcessRunner {
  private val isWindows: Boolean = sys.props("os.name").toLowerCase.contains("windows")

  def resolveRunner(): ProcessRunner = {
    if (isWindows) new WindowsProcessRunner
    else new UnixProcessRunner
  }
}
