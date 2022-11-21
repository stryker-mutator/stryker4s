package stryker4jvm.mutants

import fs2.Stream
import fs2.io.file.Path
import org.scalactic.source.Position
import stryker4jvm.config.Config
import stryker4jvm.extensions.TreeExtensions.FindExtension
import stryker4jvm.mutants.findmutants.MutantMatcherImpl
import stryker4jvm.mutants.tree.{InstrumenterOptions, MutantCollector, MutantInstrumenter}
import stryker4jvm.scalatest.LogMatchers
import stryker4jvm.testutil.Stryker4sIOSuite
import stryker4jvm.testutil.stubs.MutantFinderStub

import scala.meta.*

class AddAllMutationsTest extends Stryker4sIOSuite with LogMatchers {

  describe("failed to add mutations") {
    implicit val config = Config.default

    it("#585 (if-statement in Term.Apply)") {
      checkAllMutationsAreAdded(
        q"""SomeExecutor.createSomething(
        if (c.i.isDefined) "foo" else "bar",
        false,
      )""",
        5
      )
    }

    it("#586 (second function call with `case`)") {
      checkAllMutationsAreAdded(
        q"""serviceProvider
              .request { _ => 4 > 5 }
              .recoverWith {
                case e: Throwable =>
                  logger.info(s"Something failed")
              }""",
        4
      )
    }

    it("#776 (if-else block statement)") {
      checkAllMutationsAreAdded(
        q"""
        if (foo) bar
        else { 4 > 5 }
      """,
        5
      )
    }

    it("#776 2") {
      checkAllMutationsAreAdded(
        q"""
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
      """,
        2
      )
    }

    it("each case of pattern match") {
      checkAllMutationsAreAdded(
        q"""
        foo match {
          case _ => "break"
          case _ if high == low => baz
        }""",
        2
      )
    }

    it("try-catch-finally") {
      checkAllMutationsAreAdded(
        q"""
        def foo =
          try {
            runAndContinue("task.run")
          } catch {
            case _ => logger.error("Error during run", e)
          } finally {
            logger.info("Done")
          }""",
        3
      )
    }

    def checkAllMutationsAreAdded(tree: Stat, expectedMutations: Int)(implicit pos: Position) = {
      val source = source"class Foo { $tree }"

      val mutator = new Mutator(
        new MutantFinderStub(source),
        new MutantCollector(new TraverserImpl(), new MutantMatcherImpl()),
        new MutantInstrumenter(InstrumenterOptions.testRunner)
      )

      mutator
        .go(Stream.emit(Path("Foo.scala")))
        .asserting { case (ignored, files) =>
          ignored shouldBe Map(Path("Foo.scala") -> Vector.empty)

          val file = files.loneElement
          file.mutants.toVector.map { mutant =>
            file.mutatedSource
              .find(mutant.mutatedCode.mutatedStatement)
              .flatMap(_ => file.mutatedSource.find(p"${Lit.Int(mutant.id)}"))
              .getOrElse(
                fail(
                  s"Could not find mutant ${mutant.id} `${mutant.mutatedCode.metaData.replacement}` (original `${mutant.mutatedCode.metaData.original}`) in mutated tree ${file.mutatedSource}"
                )
              )
          }
          file.mutants.length shouldBe expectedMutations
          "Failed to instrument mutants" should not be loggedAsWarning
        }
    }
  }

}