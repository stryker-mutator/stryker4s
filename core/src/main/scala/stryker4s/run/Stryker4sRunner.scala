package stryker4s.run

import stryker4s.Stryker4s
import stryker4s.config.{Config, ConfigReader, ConsoleReporter}
import stryker4s.mutants.Mutator
import stryker4s.mutants.applymutants.ActiveMutationContext.ActiveMutationContext
import stryker4s.mutants.applymutants.{MatchBuilder, StatementTransformer}
import stryker4s.mutants.findmutants.{FileCollector, MutantFinder, MutantMatcher, SourceCollector}
import stryker4s.run.report.{CombinedReporter, ConsoleReporter, MutantRunReporter}
import stryker4s.run.threshold.ScoreStatus

import scala.meta.internal.tokenizers.PlatformTokenizerCache

trait Stryker4sRunner {

  def run(): ScoreStatus = {
    implicit val config: Config = ConfigReader.readConfig()

    // Scalameta uses a cache file->tokens that exists at a process level
    // if one file changes between runs (in the same process, eg a single SBT session) could lead to an error, so
    // it is cleaned before it starts.
    PlatformTokenizerCache.megaCache.clear()

    val collector = new FileCollector
    val stryker4s = new Stryker4s(
      collector,
      new Mutator(new MutantFinder(new MutantMatcher), new StatementTransformer, new MatchBuilder(mutationActivation)),
      resolveRunner(collector)
    )
    stryker4s.run()
  }

  def resolveRunner(collector: SourceCollector)(implicit config: Config): MutantRunner

  def mutationActivation: ActiveMutationContext

  def resolveReporters(implicit config: Config): MutantRunReporter =
    new CombinedReporter(config.reporters collect {
      case ConsoleReporter => new ConsoleReporter()
    })
}
