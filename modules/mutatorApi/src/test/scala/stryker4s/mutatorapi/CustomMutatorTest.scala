package stryker4s.mutatorapi

import stryker4s.testkit.Stryker4sSuite

import cats.data.NonEmptyVector
import scala.meta.*

class CustomMutatorTest extends Stryker4sSuite {

  /** A minimal example custom mutator, as a third-party implementation would write one: matching on `Term.Name("<")`
    * and mutating it to `Term.Name(">")`.
    */
  private class SwapLessThanMutator extends CustomMutator {
    def matcher: MutationMatcher = { case orig @ Term.Name("<") =>
      placeableTree =>
        val mutatedStatement = placeableTree.tree.asInstanceOf[Term]
        val metadata = MutantMetadata(orig.value, ">", "SwapLessThan", orig.pos, None)
        Right(NonEmptyVector.one(MutatedCode(mutatedStatement, metadata)))
    }
  }

  test("a CustomMutator's matcher should be invocable like a built-in matcher") {
    val mutator = new SwapLessThanMutator
    val tree = Term.Name("<")

    assert(mutator.matcher.isDefinedAt(tree))

    val result = mutator.matcher(tree)(PlaceableTree(tree))

    val mutations = result.value
    assertEquals(mutations.head.metadata.mutatorName, "SwapLessThan")
    assertEquals(mutations.head.metadata.replacement, ">")
  }

  test("a CustomMutator's matcher should not match unrelated trees") {
    val mutator = new SwapLessThanMutator

    assert(!mutator.matcher.isDefinedAt(Term.Name(">")))
  }
}
