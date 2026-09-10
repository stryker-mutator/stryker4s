package example

import scala.meta.*

/** Unit tests for cats-effect sequencing mutators, written first (TDD) before the mutator implementations. These cover
  * the common effect-sequencing operators `*>` and `<*`.
  */
class SequencingMutatorTest extends munit.FunSuite {
  private val swapMutator = new SequencingSwapMutator
  private val removalMutator = new SequencingRemovalMutator

  private def parseTerm(code: String): Term =
    code.parse[Term].get

  test("SequencingSwapMutator flips fa *> fb to fa <* fb") {
    val term = parseTerm("first *> second")

    assert(swapMutator.matcher.isDefinedAt(term))

    val Right(mutations) =
      swapMutator.matcher(term)(stryker4s.mutatorapi.PlaceableTree(term)): @unchecked
    assertEquals(mutations.length, 1)
    assertEquals(mutations.head.metadata.mutatorName, "SequencingSwap")
    assertEquals(mutations.head.mutatedStatement.syntax, "first <* second")
  }

  test("SequencingSwapMutator flips fa <* fb to fa *> fb") {
    val term = parseTerm("first <* second")

    assert(swapMutator.matcher.isDefinedAt(term))

    val Right(mutations) =
      swapMutator.matcher(term)(stryker4s.mutatorapi.PlaceableTree(term)): @unchecked
    assertEquals(mutations.head.mutatedStatement.syntax, "first *> second")
  }

  test("SequencingRemovalMutator removes the discarded side from fa *> fb") {
    val term = parseTerm("first *> second")

    assert(removalMutator.matcher.isDefinedAt(term))

    val Right(mutations) =
      removalMutator.matcher(term)(stryker4s.mutatorapi.PlaceableTree(term)): @unchecked
    assertEquals(mutations.length, 1)
    assertEquals(mutations.head.metadata.mutatorName, "SequencingRemoval")
    assertEquals(mutations.head.mutatedStatement.syntax, "second")
  }

  test("SequencingRemovalMutator removes the discarded side from fa <* fb") {
    val term = parseTerm("first <* second")

    assert(removalMutator.matcher.isDefinedAt(term))

    val Right(mutations) =
      removalMutator.matcher(term)(stryker4s.mutatorapi.PlaceableTree(term)): @unchecked
    assertEquals(mutations.head.mutatedStatement.syntax, "first")
  }

  test("sequencing mutators preserve complex left and right expressions") {
    val term = parseTerm("validate(value) *> persist(value)")

    val Right(swapMutations) =
      swapMutator.matcher(term)(stryker4s.mutatorapi.PlaceableTree(term)): @unchecked
    val Right(removalMutations) =
      removalMutator.matcher(term)(stryker4s.mutatorapi.PlaceableTree(term)): @unchecked

    assertEquals(swapMutations.head.mutatedStatement.syntax, "validate(value) <* persist(value)")
    assertEquals(removalMutations.head.mutatedStatement.syntax, "persist(value)")
  }

  test("sequencing mutators do not match unrelated infix operators") {
    assert(!swapMutator.matcher.isDefinedAt(parseTerm("first + second")))
    assert(!removalMutator.matcher.isDefinedAt(parseTerm("first orElse second")))
  }
}
