package stryker4s.mutants

import fs2.Stream
import fs2.io.file.Path
import munit.Location
import stryker4s.config.Config
import stryker4s.extension.TreeExtensions.FindExtension
import stryker4s.mutants.findmutants.MutantMatcherImpl
import stryker4s.mutants.tree.{InstrumenterOptions, MutantCollector, MutantInstrumenter}
import stryker4s.testkit.{LogMatchers, Stryker4sIOSuite}
import stryker4s.testutil.stubs.MutantFinderStub

import scala.meta.*

class AddAllMutationsTest extends Stryker4sIOSuite with LogMatchers {

  describe("failed to add mutations") {
    implicit val config: Config = Config.default

    test("#585 (if-statement in Term.Apply)") {
      checkAllMutationsAreAdded(
        """SomeExecutor.createSomething(
        if (c.i.isDefined) "foo" else "bar",
        false,
      )""".parseTerm,
        5
      )
    }

    test("#586 (second function call with `case`)") {
      checkAllMutationsAreAdded(
        """serviceProvider
              .request { _ => 4 > 5 }
              .recoverWith {
                case e: Throwable =>
                  logger.info(s"Something failed")
              }""".parseTerm,
        4
      )
    }

    test("#776 (if-else block statement)") {
      checkAllMutationsAreAdded(
        """
        if (foo) bar
        else { 4 > 5 }
      """.parseTerm,
        5
      )
    }

    test("#776 2") {
      checkAllMutationsAreAdded(
        """
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
      """.parseTerm,
        11
      )
    }

    test("each case of pattern match") {
      checkAllMutationsAreAdded(
        """
        foo match {
          case _ => "break"
          case _ if high == low => baz
        }""".parseStat,
        2
      )
    }

    test("try-catch-finally") {
      checkAllMutationsAreAdded(
        """
        def foo =
          try {
            runAndContinue("task.run")
          } catch {
            case _ => logger.error("Error during run", e)
          } finally {
            logger.info("Done")
          }""".parseStat,
        3
      )
    }

    def checkAllMutationsAreAdded(tree: Stat, expectedMutations: Int)(implicit loc: Location) = {
      val source = s"class Foo { ${tree.syntax} }".parseSource

      val mutator = new Mutator(
        new MutantFinderStub(source),
        new MutantCollector(new TreeTraverserImpl(), new MutantMatcherImpl()),
        new MutantInstrumenter(InstrumenterOptions.testRunner)
      )

      mutator
        .go(Stream.emit(Path("Foo.scala")))
        .asserting { case (ignored, files) =>
          assertEquals(ignored, Map(Path("Foo.scala") -> Vector.empty))

          val file = files.loneElement
          file.mutants.toVector.map { mutant =>
            file.mutatedSource
              .find(mutant.mutatedCode.mutatedStatement)
              .flatMap(_ => file.mutatedSource.find(Lit.Int(mutant.id.value)))
              .getOrElse(
                fail(
                  s"Could not find mutant ${mutant.id} `${mutant.mutatedCode.metadata.replacement}` (original `${mutant.mutatedCode.metadata.original}`) in mutated tree ${file.mutatedSource}"
                )
              )
          }
          assertEquals(file.mutants.length, expectedMutations)
          assertNotLoggedWarn("Failed to instrument mutants")
        }
    }
  }

}
