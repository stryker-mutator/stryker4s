package stryker4s.command

import cats.effect.{Deferred, ExitCode, IO, IOApp}
import cats.syntax.either.*
import pureconfig.error.ConfigReaderException
import pureconfig.generic.auto.*
import stryker4s.command.config.ProcessRunnerConfig
import stryker4s.config.ConfigReader
import stryker4s.log.Logger
import stryker4s.run.threshold.ErrorStatus

import scala.concurrent.duration.FiniteDuration

object Stryker4sMain extends IOApp {
  override def run(args: List[String]): IO[ExitCode] =
    for {
      implicit0(logger: Logger) <- Stryker4sArgumentHandler.configureLogger(args)
      processRunnerConfig <- IO.fromEither(
        ConfigReader
          .readConfigOfType[ProcessRunnerConfig]()
          .leftMap(ConfigReaderException[ProcessRunnerConfig])
      )
      timeout <- Deferred[IO, FiniteDuration]
      result <- new Stryker4sCommandRunner(processRunnerConfig, timeout).run()
    } yield result match {
      case ErrorStatus => ExitCode.Error
      case _           => ExitCode.Success
    }
}
