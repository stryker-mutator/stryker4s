package stryker4s.bsp

import stryker4s.bsp.runner.BspMutantRunner
import stryker4s.config.Config
import stryker4s.mutants.applymutants.ActiveMutationContext
import stryker4s.mutants.applymutants.ActiveMutationContext.ActiveMutationContext
import stryker4s.mutants.findmutants.SourceCollector
import stryker4s.report.Reporter
import stryker4s.run.{MutantRunner, Stryker4sRunner}

class Stryker4sBspRunner extends Stryker4sRunner {
  override def resolveRunner(collector: SourceCollector, reporter: Reporter)(implicit config: Config): MutantRunner = {
    val bspContext = BspContext.retrieveContext
    new BspMutantRunner(bspContext, collector, reporter)
  }

  override def mutationActivation: ActiveMutationContext = ActiveMutationContext.envVar
}
