package stryker4s.command

import pureconfig.error.ConfigReaderException
import stryker4s.command.config.ProcessRunnerConfig
import stryker4s.config.ConfigReader
import stryker4s.run.threshold.ErrorStatus
import pureconfig.generic.auto._
import cats.effect.IOApp
import cats.effect.{ExitCode, IO}

object Stryker4sMain extends IOApp {
  override def run(args: List[String]): IO[ExitCode] =
    IO {
      Stryker4sArgumentHandler.handleArgs(args)

      val processRunnerConfig: ProcessRunnerConfig =
        ConfigReader.readConfigOfType[ProcessRunnerConfig]() fold (
          failures => throw ConfigReaderException(failures),
          identity
      )

      val result = new Stryker4sCommandRunner(processRunnerConfig).run()

      result match {
        case ErrorStatus => ExitCode.Error
        case _           => ExitCode.Success
      }
    }
}
