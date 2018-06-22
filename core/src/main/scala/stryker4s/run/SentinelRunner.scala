package stryker4s.run

import stryker4s.Stryker4s
import stryker4s.config.{Config, ConfigReader}
import stryker4s.mutants.Mutator
import stryker4s.mutants.applymutants.{MatchBuilder, StatementTransformer}
import stryker4s.mutants.findmutants.{FileCollector, MutantFinder, MutantMatcher}
import stryker4s.run.process.ProcessRunner
import stryker4s.run.report.LogRunReporter

object Stryker4sRunner extends App {
  implicit val config: Config = ConfigReader.readConfig()

  val stryker4s = new Stryker4s(
    new FileCollector,
    new Mutator(new MutantFinder(new MutantMatcher, new MutantRegistry),
                new StatementTransformer,
                new MatchBuilder),
    new ProcessMutantRunner(ProcessRunner.resolveRunner()),
    new LogRunReporter()
  )

  stryker4s.run()
}
