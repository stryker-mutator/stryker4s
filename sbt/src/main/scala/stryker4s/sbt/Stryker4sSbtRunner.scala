package stryker4s.sbt
import stryker4s.Stryker4s
import stryker4s.config.Config
import stryker4s.mutants.Mutator
import stryker4s.mutants.applymutants.{MatchBuilder, StatementTransformer}
import stryker4s.mutants.findmutants.{MutantFinder, MutantMatcher}
import stryker4s.run.MutantRegistry

class Stryker4sSbtRunner {
  def run(): Unit = {
    implicit val config: Config = ???
    val stryker4s = new Stryker4s(
      new SbtSourceCollector(???),
      new Mutator(new MutantFinder(new MutantMatcher, new MutantRegistry),
                  new StatementTransformer,
                  new MatchBuilder),
      ???,
      ???
    )

    stryker4s.run()
  }
}
