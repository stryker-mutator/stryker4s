package stryker4s.command

import cats.effect.{ContextShift, IO}
import stryker4s.command.config.ProcessRunnerConfig
import stryker4s.command.runner.ProcessMutantRunner
import stryker4s.config.Config
import stryker4s.mutants.applymutants.ActiveMutationContext
import stryker4s.mutants.applymutants.ActiveMutationContext.ActiveMutationContext
import stryker4s.mutants.findmutants.SourceCollector
import stryker4s.report.Reporter
import stryker4s.run.process.ProcessRunner
import stryker4s.run.{MutantRunner, Stryker4sRunner}

class Stryker4sCommandRunner(processRunnerConfig: ProcessRunnerConfig)(implicit cs: ContextShift[IO])
    extends Stryker4sRunner {
  override val mutationActivation: ActiveMutationContext = ActiveMutationContext.envVar

  override def resolveRunner(collector: SourceCollector, reporter: Reporter)(implicit config: Config): MutantRunner =
    new ProcessMutantRunner(processRunnerConfig.testRunner, ProcessRunner(), collector, reporter)
}
