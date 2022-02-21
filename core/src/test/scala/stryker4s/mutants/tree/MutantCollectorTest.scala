package stryker4s.mutants.tree

import cats.data.NonEmptyVector
import cats.syntax.all.*
import mutationtesting.{Location, Position}
import stryker4s.model.{MutantMetadata, MutatedCode, PlaceableTree}
import stryker4s.mutants.Traverser
import stryker4s.testutil.Stryker4sSuite

import java.util.concurrent.atomic.AtomicInteger
import scala.meta.*
import stryker4s.scalatest.LogMatchers

class MutantCollectorTest extends Stryker4sSuite with LogMatchers {

  describe("onEnter") {
    it("should only call 'isDefinedAt' on the PartialFunction once") {
      val onEnterCounter = new AtomicInteger(0)
      val pf: PartialFunction[Tree, PlaceableTree => Either[IgnoredMutations, Mutations]] = {
        // if-guard is always true and counts how many times it is called
        case _ if onEnterCounter.incrementAndGet() != -1 =>
          _ =>
            NonEmptyVector
              .one(
                MutatedCode(q"foo", MutantMetadata("<", ">", "GreaterThan", Location(Position(0, 7), Position(0, 8))))
              )
              .asRight
      }
      val sut = new MutantCollector(new TraverserStub(q"foo", pf))
      val tree = q"def bar = 15 > 14"

      val results = sut(tree)
      val onEnterCalled = onEnterCounter.get()

      def lengthOfTree(t: Tree): Int = 1 + t.children.map(lengthOfTree(_)).sum
      onEnterCalled shouldBe lengthOfTree(tree)
      onEnterCalled shouldBe >(1)
      results.size shouldBe onEnterCalled
    }
  }

  class TraverserStub(
      termToMatch: Term,
      pf: PartialFunction[Tree, PlaceableTree => Either[IgnoredMutations, Mutations]]
  ) extends Traverser {
    override def canPlace(currentTree: Term): Option[Term] =
      Some(termToMatch)
    override def findMutations: PartialFunction[Tree, PlaceableTree => Either[IgnoredMutations, Mutations]] = pf
  }
}
