package stryker4s.command

import cats.effect.{ExitCode, IO, IOApp}
import pureconfig.error.ConfigReaderException
import pureconfig.generic.auto._
import stryker4s.command.config.ProcessRunnerConfig
import stryker4s.config.ConfigReader
import stryker4s.run.threshold.ErrorStatus

object Stryker4sMain extends IOApp {
  override def run(args: List[String]): IO[ExitCode] =
    IO {
      Stryker4sArgumentHandler.handleArgs(args)

      val processRunnerConfig: ProcessRunnerConfig =
        ConfigReader.readConfigOfType[ProcessRunnerConfig]() match {
          case Left(failures) => throw ConfigReaderException(failures)
          case Right(config)  => config
        }

      val result = new Stryker4sCommandRunner(processRunnerConfig).run()

      result match {
        case ErrorStatus => ExitCode.Error
        case _           => ExitCode.Success
      }
    }
}
