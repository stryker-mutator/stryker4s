package stryker4s.mutants.tree

import cats.data.NonEmptyVector
import cats.syntax.either.*
import cats.syntax.option.*
import mutationtesting.{Location, Position}
import stryker4s.config.Config
import stryker4s.model.{MutantMetadata, MutatedCode, PlaceableTree}
import stryker4s.mutants.findmutants.{MutantMatcher, MutantMatcherImpl}
import stryker4s.mutants.{TreeTraverser, TreeTraverserImpl}
import stryker4s.testkit.{LogMatchers, Stryker4sSuite}

import java.util.concurrent.atomic.AtomicInteger
import scala.meta.*

class MutantCollectorTest extends Stryker4sSuite with LogMatchers {

  describe("onEnter") {
    test("should only call 'isDefinedAt' on the PartialFunction once") {
      val onEnterCounter = new AtomicInteger(0)
      val matcher = new MutantMatcher {
        // if-guard is always true and counts how many times it is called
        override def allMatchers = {
          case _ if onEnterCounter.incrementAndGet() != -1 =>
            (_: PlaceableTree) =>
              NonEmptyVector
                .one(
                  MutatedCode(q"foo", MutantMetadata("<", ">", "GreaterThan", Location(Position(0, 7), Position(0, 8))))
                )
                .asRight[IgnoredMutations]
        }
      }
      val sut = new MutantCollector(new TraverserStub(q"foo"), matcher)
      val tree = q"def bar = 15 > 14"

      val (_, results) = sut(tree)
      val onEnterCalled = onEnterCounter.get()

      def lengthOfTree(t: Tree): Int = 1 + t.children.map(lengthOfTree(_)).sum
      assertEquals(onEnterCalled, lengthOfTree(tree))
      assert(onEnterCalled > 1)
      assertEquals(results.size, 1)
    }
    class TraverserStub(
        termToMatch: Term
    ) extends TreeTraverser {
      override def canPlace(currentTree: Tree): Option[Term] = termToMatch.some
    }
  }

  describe("apply") {
    implicit val config = Config.default

    test("should return the mutated code") {

      val sut = new MutantCollector(new TreeTraverserImpl(), new MutantMatcherImpl())
      val tree = q"def bar = 15 > 14"

      val (ignored, found) = sut(tree)
      assertEquals(ignored, Vector.empty)
      assertEquals(found.size, 1)
    }

  }

}
