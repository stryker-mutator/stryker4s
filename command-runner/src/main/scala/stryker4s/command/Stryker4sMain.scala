package stryker4s.command

import cats.effect.{Deferred, ExitCode, IO, IOApp}
import pureconfig.error.ConfigReaderException
import pureconfig.generic.auto._
import stryker4s.command.config.ProcessRunnerConfig
import stryker4s.config.ConfigReader
import stryker4s.log.Logger
import stryker4s.run.threshold.ErrorStatus

import scala.concurrent.duration.FiniteDuration

object Stryker4sMain extends IOApp {
  override def run(args: List[String]): IO[ExitCode] =
    for {
      implicit0(logger: Logger) <- Stryker4sArgumentHandler.configureLogger(args)
      processRunnerConfig <- IO(ConfigReader.readConfigOfType[ProcessRunnerConfig]() match {
        case Left(failures) => throw ConfigReaderException[ProcessRunnerConfig](failures)
        case Right(config)  => config
      })
      timeout <- Deferred[IO, FiniteDuration]
      result <- new Stryker4sCommandRunner(processRunnerConfig, timeout).run()
    } yield result match {
      case ErrorStatus => ExitCode.Error
      case _           => ExitCode.Success
    }
}
