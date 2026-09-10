package example

import scala.meta.*

/** Unit tests for fallback-path mutators, written first (TDD) before the mutator implementations. These cover the
  * high-feasibility cats-effect fallback shape `primary.orElse(fallback)`.
  */
class FallbackMutatorTest extends munit.FunSuite {
  private val removalMutator = new FallbackRemovalMutator
  private val inversionMutator = new FallbackInversionMutator

  private def parseTerm(code: String): Term =
    code.parse[Term].get

  test("FallbackRemovalMutator drops .orElse(fallback), leaving the primary effect") {
    val term = parseTerm("primary.orElse(fallback)")

    assert(removalMutator.matcher.isDefinedAt(term))

    val Right(mutations) =
      removalMutator.matcher(term)(stryker4s.mutatorapi.PlaceableTree(term)): @unchecked
    assertEquals(mutations.length, 1)
    assertEquals(mutations.head.metadata.mutatorName, "FallbackRemoval")
    assertEquals(mutations.head.mutatedStatement.syntax, "primary")
  }

  test("FallbackInversionMutator replaces primary.orElse(fallback) with fallback") {
    val term = parseTerm("primary.orElse(fallback)")

    assert(inversionMutator.matcher.isDefinedAt(term))

    val Right(mutations) =
      inversionMutator.matcher(term)(stryker4s.mutatorapi.PlaceableTree(term)): @unchecked
    assertEquals(mutations.length, 1)
    assertEquals(mutations.head.metadata.mutatorName, "FallbackInversion")
    assertEquals(mutations.head.mutatedStatement.syntax, "fallback")
  }

  test("fallback mutators preserve complex receiver and fallback expressions") {
    val term = parseTerm("primary.flatMap(next).orElse(makeFallback(value))")

    val Right(removalMutations) =
      removalMutator.matcher(term)(stryker4s.mutatorapi.PlaceableTree(term)): @unchecked
    val Right(inversionMutations) =
      inversionMutator.matcher(term)(stryker4s.mutatorapi.PlaceableTree(term)): @unchecked

    assertEquals(removalMutations.head.mutatedStatement.syntax, "primary.flatMap(next)")
    assertEquals(inversionMutations.head.mutatedStatement.syntax, "makeFallback(value)")
  }

  test("fallback mutators do not match unrelated method calls") {
    assert(!removalMutator.matcher.isDefinedAt(parseTerm("primary.handleErrorWith(fallback)")))
    assert(!inversionMutator.matcher.isDefinedAt(parseTerm("primary.map(f)")))
  }

  test("fallback mutators do not match .orElse without exactly one fallback argument") {
    assert(!removalMutator.matcher.isDefinedAt(parseTerm("primary.orElse")))
    assert(!inversionMutator.matcher.isDefinedAt(parseTerm("primary.orElse")))
  }
}
