package stryker4s.run

import stryker4s.config.{CommandRunner, Config}
import stryker4s.mutants.applymutants.ActiveMutationContext
import stryker4s.mutants.applymutants.ActiveMutationContext.ActiveMutationContext
import stryker4s.mutants.findmutants.SourceCollector
import stryker4s.run.process.{Command, ProcessMutantRunner, ProcessRunner}
import stryker4s.run.threshold.ErrorStatus

object Stryker4sCommandRunner extends App with Stryker4sRunner {

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

  override def resolveRunner(collector: SourceCollector)(implicit config: Config): MutantRunner =
    config.testRunner match {
      case CommandRunner(command, args) =>
        new ProcessMutantRunner(Command(command, args), ProcessRunner(), collector)
    }
}
