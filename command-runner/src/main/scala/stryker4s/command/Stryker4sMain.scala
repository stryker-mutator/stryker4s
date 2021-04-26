package stryker4s.command

import scala.concurrent.duration.FiniteDuration

import cats.effect.{Deferred, ExitCode, IO, IOApp}
import pureconfig.error.ConfigReaderException
import pureconfig.generic.auto._
import stryker4s.command.config.ProcessRunnerConfig
import stryker4s.config.ConfigReader
import stryker4s.log.{Logger, Slf4jLogger}
import stryker4s.run.threshold.ErrorStatus

object Stryker4sMain extends IOApp {
  override def run(args: List[String]): IO[ExitCode] =
    for {
      implicit0(logger: Logger) <- IO(new Slf4jLogger())
      _ <- IO(Stryker4sArgumentHandler.handleArgs(args))
      processRunnerConfig <- IO(ConfigReader.readConfigOfType[ProcessRunnerConfig]() match {
        case Left(failures) => throw ConfigReaderException(failures)
        case Right(config)  => config
      })
      timeout <- Deferred[IO, FiniteDuration]
      result <- new Stryker4sCommandRunner(processRunnerConfig, timeout).run()
    } yield result match {
      case ErrorStatus => ExitCode.Error
      case _           => ExitCode.Success
    }
}
