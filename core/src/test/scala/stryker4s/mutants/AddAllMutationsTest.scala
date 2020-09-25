package stryker4s.mutants

import scala.meta._

import stryker4s.config.Config
import stryker4s.extension.TreeExtensions._
import stryker4s.mutants.applymutants.{ActiveMutationContext, MatchBuilder, StatementTransformer}
import stryker4s.mutants.findmutants.MutantMatcher
import stryker4s.scalatest.LogMatchers
import stryker4s.testutil.Stryker4sSuite

class AddAllMutationsTest extends Stryker4sSuite with LogMatchers {

  describe("failed to add mutations") {
    implicit val config = Config.default

    it("#586 (second function call with `case`)") {
      checkAllMutationsAreAdded(q"""serviceProvider
              .request { _ => 4 > 5 }
              .recoverWith {
                case e: Throwable =>
                  logger.info(s"Something failed")
              }""")
    }

    def checkAllMutationsAreAdded(tree: Stat) = {
      val source = source"class Foo { $tree }"
      val foundMutants = source.collect(new MutantMatcher().allMatchers).flatten.flatten
      val transformed = new StatementTransformer().transformSource(source, foundMutants)
      val mutatedTree = new MatchBuilder(ActiveMutationContext.envVar).buildNewSource(transformed)
      foundMutants.foreach(mutant =>
        mutatedTree
          .find(mutant.mutated)
          .map(_ => succeed)
          .getOrElse(
            fail(s"Could not find mutation ${mutant.original} (${mutant.mutated}) in mutated tree ${mutatedTree}")
          )
      )
      "Failed to add mutation(s)" should not be loggedAsWarning
    }
  }
}
