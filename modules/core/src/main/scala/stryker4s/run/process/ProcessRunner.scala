package stryker4s.run.process

import cats.effect.IO
import fs2.io.file.Path
import fs2.io.process.ProcessBuilder
import stryker4s.config.Config
import stryker4s.log.Logger

import scala.util.{Properties, Try}

abstract class ProcessRunner(implicit log: Logger) {
  def apply(command: Command, workingDir: Path, envVar: (String, String)*)(implicit config: Config): IO[Try[Int]] = {
    val logger: String => IO[Unit] =
      if (config.debug.logTestRunnerStdout) m => IO(log.debug(s"testrunner: $m"))
      else _ => IO.unit

    ProcessResource
      .fromProcessBuilder(
        ProcessBuilder(command.command, command.args)
          .withWorkingDirectory(workingDir)
          .withExtraEnv(envVar.toMap),
        logger
      )
      .use(_.exitValue)
      .attempt
      .map(_.toTry)
  }
}

object ProcessRunner {

  def apply()(implicit log: Logger): ProcessRunner = {
    if (Properties.isWin) new WindowsProcessRunner
    else new UnixProcessRunner
  }
}
