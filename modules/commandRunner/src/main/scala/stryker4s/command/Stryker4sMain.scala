package stryker4s.command

import cats.effect.{Deferred, ExitCode, IO, IOApp}
import stryker4s.command.config.ProcessRunnerConfig
import stryker4s.log.Logger
import stryker4s.run.threshold.ErrorStatus

import scala.concurrent.duration.FiniteDuration

object Stryker4sMain extends IOApp {
  override def run(args: List[String]): IO[ExitCode] =
    for {
      implicit0(logger: Logger) <- Stryker4sArgumentHandler.configureLogger(args)
      processRunnerConfig: ProcessRunnerConfig <- IO.raiseError(new NotImplementedError())
      timeout <- Deferred[IO, FiniteDuration]
      result <- new Stryker4sCommandRunner(processRunnerConfig, timeout, args).run()
    } yield result match {
      case ErrorStatus => ExitCode.Error
      case _           => ExitCode.Success
    }
}
