package stryker4jvm.mutator.scala

import org.scalatest.funspec.AnyFunSpec
import stryker4jvm.mutator.scala.scalatest.FileUtil
import fs2.io.file.Path
import java.nio.file.NoSuchFileException
import stryker4jvm.core.config.LanguageMutatorConfig
import scala.meta.*
import cats.syntax.either.*
import cats.syntax.option.*
import scala.collection.JavaConverters.*
import java.util as ju
import stryker4jvm.mutator.scala.testutil.Stryker4sSuite

import java.util.concurrent.atomic.AtomicInteger
import cats.data.NonEmptyVector
import stryker4jvm.core.model.MutatedCode
import stryker4jvm.core.model.MutantMetaData
import stryker4jvm.core.model.elements.Location
import stryker4jvm.core.model

import stryker4jvm.mutator.scala.Traverser

import stryker4jvm.core.model.CollectedMutants.IgnoredMutation

class ScalaCollectorTest extends Stryker4sSuite {

  implicit val log = new ScalaLogger()
  val col = new ScalaCollector(
    traverser = new TraverserImpl(),
    matcher = new MutantMatcherImpl(config = new LanguageMutatorConfig(scala.collection.Set().asJava))
  )

  describe("onEnter") {
    it("should only call 'isDefinedAt' on the PartialFunction once") {
      val onEnterCounter = new AtomicInteger(0)
      val matcher = new MutantMatcher {
        // if-guard is always true and counts how many times it is called
        override def allMatchers = {
          case _ if onEnterCounter.incrementAndGet() != -1 =>
            (_: PlaceableTree) =>
              Right(
                Vector(
                  new MutatedCode[ScalaAST](
                    new ScalaAST(value = q"foo"),
                    new MutantMetaData(
                      "<",
                      ">",
                      "GreaterThan",
                      new Location(new model.elements.Position(0, 7), new model.elements.Position(0, 8))
                    )
                  )
                )
              )
        }
      }

      val custom_col = new ScalaCollector(
        traverser = new TraverserStub(q"foo"),
        matcher = matcher
      )
      val tree = q"def bar = 15 > 14"

      val results = custom_col.collect(new ScalaAST(value = tree))
      val onEnterCalled = onEnterCounter.get()

      def lengthOfTree(t: Tree): Int = 1 + t.children.map(lengthOfTree(_)).sum
      onEnterCalled shouldBe lengthOfTree(tree)
      onEnterCalled shouldBe >(1)
      results.mutations.size shouldBe 1
    }
    class TraverserStub(
        termToMatch: Term
    ) extends Traverser {
      override def canPlace(currentTree: Tree): Option[Term] = termToMatch.some
    }
  }

  describe("apply") {
    it("should return the mutated code") {
      val tree = new ScalaAST(value = q"def bar = 15 > 14")
      val found = col.collect(tree);

      found.ignoredMutations shouldBe empty
      found.mutations.size shouldBe 1
    }
  }
}
