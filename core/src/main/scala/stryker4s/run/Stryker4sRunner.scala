package stryker4s.run

import stryker4s.Stryker4s
import stryker4s.config.{Config, ConfigReader}
import stryker4s.mutants.Mutator
import stryker4s.mutants.applymutants.ActiveMutationContext.ActiveMutationContext
import stryker4s.mutants.applymutants.{MatchBuilder, StatementTransformer}
import stryker4s.mutants.findmutants.{FileCollector, MutantFinder, MutantMatcher, SourceCollector}
import stryker4s.run.process.ProcessRunner
import stryker4s.report.Reporter
import stryker4s.run.threshold.ScoreStatus

import scala.meta.internal.tokenizers.PlatformTokenizerCache

trait Stryker4sRunner {

  def run(): ScoreStatus = {
    implicit val config: Config = ConfigReader.readConfig()

    // Scalameta uses a cache file->tokens that exists at a process level
    // if one file changes between runs (in the same process, eg a single SBT session) could lead to an error, so
    // it is cleaned before it starts.
    PlatformTokenizerCache.megaCache.clear()

    val collector = new FileCollector(ProcessRunner())
    val stryker4s = new Stryker4s(
      collector,
      new Mutator(new MutantFinder(new MutantMatcher), new StatementTransformer, new MatchBuilder(mutationActivation)),
      resolveRunner(collector),
      new Reporter()
    )
    stryker4s.run()
  }

  def resolveRunner(collector: SourceCollector)(implicit config: Config): MutantRunner

  def mutationActivation: ActiveMutationContext

}
