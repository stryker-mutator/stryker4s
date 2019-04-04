package stryker4s.run

import better.files.File
import pureconfig.error.ConfigReaderException
import stryker4s.config.{Config, ConfigReader, ProcessRunnerConfig}
import stryker4s.mutants.applymutants.ActiveMutationContext
import stryker4s.mutants.applymutants.ActiveMutationContext.ActiveMutationContext
import stryker4s.mutants.findmutants.SourceCollector
import stryker4s.report.Reporter
import stryker4s.run.process.ProcessRunner
import stryker4s.run.threshold.ErrorStatus

object Stryker4sCommandRunner extends App with Stryker4sRunner {

  private[this] val processRunnerConfig: ProcessRunnerConfig =
    ConfigReader.readConfig[ProcessRunnerConfig]() match {
      case Left(failures) => throw ConfigReaderException(failures)
      case Right(config)  => config
    }

  Stryker4sArgumentHandler.handleArgs(args)

  override val mutationActivation: ActiveMutationContext = ActiveMutationContext.envVar

  val result = run()

  val exitCode = result match {
    case ErrorStatus => 1
    case _           => 0
  }
  this.exit()

  private def exit(): Unit = {
    sys.exit(exitCode)
  }

  override def resolveRunner(collector: SourceCollector, reporter: Reporter)(implicit config: Config): MutantRunner =
    new ProcessMutantRunner(processRunnerConfig.testRunnerCommand, ProcessRunner(), collector, reporter)
}
