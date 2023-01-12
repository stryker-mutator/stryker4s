package stryker4jvm.command

import cats.effect.{Deferred, ExitCode, IO, IOApp}
import cats.syntax.either.*
import pureconfig.error.ConfigReaderException
import pureconfig.generic.auto.*
import stryker4jvm.command.config.ProcessRunnerConfig
import stryker4jvm.config.ConfigReader
import stryker4jvm.logging.FansiLogger
import stryker4jvm.run.threshold.ErrorStatus

import scala.concurrent.duration.FiniteDuration

object Stryker4jvmMain extends IOApp {
  override def run(args: List[String]): IO[ExitCode] =
    for {
      implicit0(logger: FansiLogger) <- Stryker4jvmArgumentHandler.configureLogger(args)
      processRunnerConfig <- IO.fromEither(
        ConfigReader
          .readConfigOfType[ProcessRunnerConfig]()
          .leftMap(ConfigReaderException[ProcessRunnerConfig])
      )
      timeout <- Deferred[IO, FiniteDuration]
      result <- new Stryker4jvmCommandRunner(processRunnerConfig, timeout).run()
    } yield result match {
      case ErrorStatus => ExitCode.Error
      case _           => ExitCode.Success
    }
}
