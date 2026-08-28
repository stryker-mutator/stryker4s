package stryker4s.mutatorapi

import stryker4s.testkit.Stryker4sSuite

import cats.data.NonEmptyVector
import scala.meta.*

class CustomMutatorTest extends Stryker4sSuite {

  /** A minimal example custom mutator, written the way a third-party implementation should be: matching a sub-term
    * (here `Term.Name("<")`) and building the mutated statement with `PlaceableTree.substitute`, so the mutation is
    * applied in place and the surrounding statement is preserved.
    */
  private class SwapLessThanMutator extends CustomMutator {
    def matcher: MutationMatcher = { case orig @ Term.Name("<") =>
      placeableTree =>
        val replacement = Term.Name(">")
        val metadata = MutantMetadata(orig.value, ">", "SwapLessThan", orig.pos, None)
        Right(NonEmptyVector.one(MutatedCode(placeableTree.substitute(orig, replacement), metadata)))
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

  test("a CustomMutator's matcher should keep the statement surrounding the matched sub-term") {
    val mutator = new SwapLessThanMutator
    val statement = "a < b".parseTerm
    val op = statement.collect { case t @ Term.Name("<") => t }.head

    val mutations = mutator.matcher(op)(PlaceableTree(statement)).value

    assertEquals(mutations.head.mutatedStatement.syntax, "a > b")
  }

  test("a CustomMutator's matcher should not match unrelated trees") {
    val mutator = new SwapLessThanMutator

    assert(!mutator.matcher.isDefinedAt(Term.Name(">")))
  }
}
