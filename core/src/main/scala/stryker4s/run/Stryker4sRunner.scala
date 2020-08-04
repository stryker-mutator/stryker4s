package stryker4s.run

import cats.effect.{ContextShift, IO}
import stryker4s.Stryker4s
import stryker4s.config.{Config, ConfigReader}
import stryker4s.mutants.Mutator
import stryker4s.mutants.applymutants.ActiveMutationContext.ActiveMutationContext
import stryker4s.mutants.applymutants.{MatchBuilder, StatementTransformer}
import stryker4s.mutants.findmutants.{FileCollector, MutantFinder, MutantMatcher, SourceCollector}
import stryker4s.report.Reporter
import stryker4s.run.process.ProcessRunner
import stryker4s.run.threshold.ScoreStatus

trait Stryker4sRunner {
  def run()(implicit cs: ContextShift[IO]): ScoreStatus = {
    implicit val config: Config = ConfigReader.readConfig()

    val collector = new FileCollector(ProcessRunner())
    val stryker4s = new Stryker4s(
      collector,
      new Mutator(new MutantFinder(new MutantMatcher), new StatementTransformer, new MatchBuilder(mutationActivation)),
      resolveRunner(collector, new Reporter())
    )
    stryker4s.run()
  }

  def resolveRunner(collector: SourceCollector, reporter: Reporter)(implicit config: Config): MutantRunner

  def mutationActivation: ActiveMutationContext
}
