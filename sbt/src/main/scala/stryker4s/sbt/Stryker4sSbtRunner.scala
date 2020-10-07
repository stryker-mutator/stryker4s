package stryker4s.sbt

import cats.effect.{ContextShift, IO, Timer}
import sbt._
import stryker4s.config.Config
import stryker4s.mutants.applymutants.ActiveMutationContext
import stryker4s.mutants.applymutants.ActiveMutationContext.ActiveMutationContext
import stryker4s.mutants.findmutants.SourceCollector
import stryker4s.report.Reporter
import stryker4s.run.{MutantRunner, Stryker4sRunner}
import stryker4s.sbt.runner.SbtMutantRunner

/** This Runner run Stryker mutations in a single SBT session
  *
  * @param state SBT project state (contains all the settings about the project)
  */
class Stryker4sSbtRunner(state: State)(implicit timer: Timer[IO], cs: ContextShift[IO]) extends Stryker4sRunner {
  override def resolveRunner(collector: SourceCollector, reporter: Reporter)(implicit config: Config): MutantRunner =
    new SbtMutantRunner(state, collector, reporter)

  override val mutationActivation: ActiveMutationContext = ActiveMutationContext.sysProps
}
