package stryker4s.mutants

import scala.meta._

import org.scalactic.source.Position
import stryker4s.config.Config
import stryker4s.extension.TreeExtensions._
import stryker4s.mutants.applymutants.{ActiveMutationContext, MatchBuilder, StatementTransformer}
import stryker4s.mutants.findmutants.MutantMatcher
import stryker4s.scalatest.LogMatchers
import stryker4s.testutil.Stryker4sSuite

class AddAllMutationsTest extends Stryker4sSuite with LogMatchers {

  describe("failed to add mutations") {
    implicit val config = Config.default

    it("#585 (if-statement in Term.Apply)") {
      checkAllMutationsAreAdded(q"""SomeExecutor.createSomething(
        if (c.i.isDefined) "foo" else "bar",
        false,
      )""")
    }

    it("#586 (second function call with `case`)") {
      checkAllMutationsAreAdded(q"""serviceProvider
              .request { _ => 4 > 5 }
              .recoverWith {
                case e: Throwable =>
                  logger.info(s"Something failed")
              }""")
    }

    it("#776 (if-else block statement)") {
      checkAllMutationsAreAdded(q"""
        if (foo) bar
        else { 4 > 5 }
      """)
    }

    it("#776 2") {
      checkAllMutationsAreAdded(q"""
        try {
          val (p1, s, rs1) = runSeqCmds(sut, as.s, as.seqCmds)
          val l1 = s"Initial State:\n \nSequential Commands:\n"
          if (as.parCmds.isEmpty) p1 :| l1
          else
            propAnd(
              p1.flatMap { r => if (!r.success) finalize; Prop(prms => r) } :| l1, {
                try {
                  val (p2, rs2) = runParCmds(sut, s, as.parCmds)
                  val l2 = rs2.map(prettyCmdsRes(_, maxLength)).mkString("\n\n")
                  p2 :| l1 :| s"Parallel Commands (starting in state = )\n"
                } finally finalize
              }
            )
        } finally if (as.parCmds.isEmpty) finalize
      """)
    }

    it("each case of pattern match") {
      checkAllMutationsAreAdded(q"""
        foo match {
          case _ => "break"
          case _ if high == low => baz
        }""")
    }

    it("try-catch-finally") {
      checkAllMutationsAreAdded(q"""
        def foo =
          try {
            runAndContinue("task.run")
          } catch {
            case _ => logger.error("Error during run", e)
          } finally {
            logger.info("Done")
          }""")
    }

    def checkAllMutationsAreAdded(tree: Stat)(implicit pos: Position) = {
      val source = source"class Foo { $tree }"
      val foundMutants = source.collect(new MutantMatcher().allMatchers).flatten.collect { case Right(v) => v }
      val transformed = new StatementTransformer().transformSource(source, foundMutants)
      val mutatedTree = new MatchBuilder(ActiveMutationContext.testRunner).buildNewSource(transformed)
      transformed.transformedStatements
        .flatMap(_.mutantStatements)
        .foreach { mutantStatement =>
          mutatedTree
            .find(p"Some(${Lit.Int(mutantStatement.id)})")
            .getOrElse(
              fail {
                val mutant = foundMutants.find(_.id == mutantStatement.id).get
                s"Could not find mutation ${mutant.id} '${mutant.mutated}' (original '${mutant.original}') in mutated tree ${mutatedTree}"
              }
            )
        }
      "Failed to add mutation(s)" should not be loggedAsWarning
    }
  }
}
