package stryker4s.command

import pureconfig.error.ConfigReaderException
import stryker4s.command.config.ProcessRunnerConfig
import stryker4s.config.ConfigReader
import stryker4s.run.threshold.ErrorStatus
import pureconfig.generic.auto._
import scala.concurrent.ExecutionContext.Implicits.global

object Stryker4sMain extends App {
  Stryker4sArgumentHandler.handleArgs(args)

  private[this] val processRunnerConfig: ProcessRunnerConfig = {
    ConfigReader.readConfigOfType[ProcessRunnerConfig]() fold (
      failures => throw ConfigReaderException(failures),
      identity
    )
  }

  val result = new Stryker4sCommandRunner(processRunnerConfig).run()

  val exitCode = result match {
    case ErrorStatus => 1
    case _           => 0
  }

  this.exit()

  private def exit(): Unit = {
    sys.exit(exitCode)
  }
}
