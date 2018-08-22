package stryker4s.run

import stryker4s.Stryker4s
import stryker4s.config.{CommandRunner, Config, ConfigReader}
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
    resolveRunner(),
    new LogRunReporter()
  )

  stryker4s.run()

  private def resolveRunner()(implicit config: Config): MutantRunner = {
    config.testRunner match {
      case CommandRunner(command) => new ProcessMutantRunner(command, ProcessRunner.resolveRunner())
    }
  }
}
