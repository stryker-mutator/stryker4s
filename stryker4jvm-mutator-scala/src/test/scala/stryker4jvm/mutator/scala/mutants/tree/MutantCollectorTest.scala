package stryker4jvm.mutator.scala.mutants.tree

import cats.data.NonEmptyVector
import cats.syntax.either.*
import cats.syntax.option.*
import mutationtesting.{Location, Position}
import stryker4jvm.mutator.scala.config.Config
import stryker4jvm.core.model.{MutantMetaData, MutatedCode}
import stryker4jvm.mutator.scala.model.PlaceableTree
import stryker4jvm.mutator.scala.mutants.findmutants.{MutantMatcher, MutantMatcherImpl}
import stryker4jvm.mutator.scala.mutants.{IgnoredMutations, Traverser, TraverserImpl}
import stryker4jvm.mutator.scala.scalatest.LogMatchers
import stryker4jvm.mutator.scala.testutil.Stryker4sSuite

import java.util.concurrent.atomic.AtomicInteger
import scala.meta.*

class MutantCollectorTest extends Stryker4sSuite with LogMatchers {

  describe("onEnter") {
    it("should only call 'isDefinedAt' on the PartialFunction once") {
      val onEnterCounter = new AtomicInteger(0)
      val matcher = new MutantMatcher {
        // if-guard is always true and counts how many times it is called
        override def allMatchers = {
          case _ if onEnterCounter.incrementAndGet() != -1 =>
            (_: PlaceableTree) =>
              NonEmptyVector
                .one(
                  new MutatedCode(
                    q"foo".asInstanceOf[Term],
                    new MutantMetaData("<", ">", "GreaterThan", Location(Position(0, 7), Position(0, 8)))
                  )
                )
                .asRight[IgnoredMutations]
        }
      }
      val sut = new MutantCollector(new TraverserStub(q"foo"), matcher)
      val tree = q"def bar = 15 > 14"

      val (_, results) = sut(tree)
      val onEnterCalled = onEnterCounter.get()

      def lengthOfTree(t: Tree): Int = 1 + t.children.map(lengthOfTree(_)).sum
      onEnterCalled shouldBe lengthOfTree(tree)
      onEnterCalled shouldBe >(1)
      results.size shouldBe 1
    }
    class TraverserStub(
        termToMatch: Term
    ) extends Traverser {
      override def canPlace(currentTree: Tree): Option[Term] = termToMatch.some
    }
  }

  describe("apply") {
    implicit val config = Config.default

    it("should return the mutated code") {

      val sut = new MutantCollector(new TraverserImpl(), new MutantMatcherImpl())
      val tree = q"def bar = 15 > 14"

      val (ignored, found) = sut(tree)
      ignored shouldBe empty
      found.size shouldBe 1
    }

  }

}
