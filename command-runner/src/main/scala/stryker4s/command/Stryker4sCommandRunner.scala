package stryker4s.command

import stryker4s.command.config.ProcessRunnerConfig
import stryker4s.command.runner.ProcessMutantRunner
import stryker4s.config.Config
import stryker4s.mutants.applymutants.ActiveMutationContext
import stryker4s.mutants.applymutants.ActiveMutationContext.ActiveMutationContext
import stryker4s.mutants.findmutants.SourceCollector
import stryker4s.report.Reporter
import stryker4s.run.process.ProcessRunner
import stryker4s.run.{MutantRunner, Stryker4sRunner}
import cats.effect.ContextShift
import cats.effect.IO

class Stryker4sCommandRunner(processRunnerConfig: ProcessRunnerConfig)(implicit cs: ContextShift[IO])
    extends Stryker4sRunner {
  override val mutationActivation: ActiveMutationContext = ActiveMutationContext.envVar

  override def resolveRunner(collector: SourceCollector, reporter: Reporter)(implicit config: Config): MutantRunner =
    new ProcessMutantRunner(processRunnerConfig.testRunner, ProcessRunner(), collector, reporter)
}
