package example

import scala.meta.*

/** Unit tests for [[HeadLastSwapMutator]]'s matcher logic, written first (TDD) before the mutator implementation.
  */
class HeadLastSwapMutatorTest extends munit.FunSuite {
  private val mutator = new HeadLastSwapMutator

  private def parseTerm(code: String): Term =
    code.parse[Term].get

  private def mutate(code: String): String = {
    val term = parseTerm(code)
    val Right(mutations) = mutator.matcher(term)(stryker4s.mutatorapi.PlaceableTree(term)): @unchecked
    assertEquals(mutations.length, 1)
    assertEquals(mutations.head.metadata.mutatorName, "HeadLastSwap")
    mutations.head.mutatedStatement.syntax
  }

  test("swaps .head for .last") {
    assertEquals(mutate("discounts.head"), "discounts.last")
  }

  test("swaps .last for .head") {
    assertEquals(mutate("discounts.last"), "discounts.head")
  }

  test("swaps .headOption for .lastOption") {
    assertEquals(mutate("discounts.headOption"), "discounts.lastOption")
  }

  test("swaps .lastOption for .headOption") {
    assertEquals(mutate("discounts.lastOption"), "discounts.headOption")
  }

  test("swaps .init for .tail") {
    assertEquals(mutate("discounts.init"), "discounts.tail")
  }

  test("swaps .tail for .init") {
    assertEquals(mutate("discounts.tail"), "discounts.init")
  }

  test("records the endpoint it swapped in the mutant metadata") {
    val term = parseTerm("discounts.head")
    val Right(mutations) = mutator.matcher(term)(stryker4s.mutatorapi.PlaceableTree(term)): @unchecked

    assertEquals(mutations.head.metadata.original, "head")
    assertEquals(mutations.head.metadata.replacement, "last")
  }

  test("keeps the statement surrounding the matched sub-term") {
    val statement = parseTerm("discounts.sorted.head + 1")
    val term = statement.collect { case t: Term if t.syntax == "discounts.sorted.head" => t }.head

    val Right(mutations) = mutator.matcher(term)(stryker4s.mutatorapi.PlaceableTree(statement)): @unchecked
    assertEquals(mutations.head.mutatedStatement.syntax, "discounts.sorted.last + 1")
  }

  test("does not match unrelated selections") {
    assert(!mutator.matcher.isDefinedAt(parseTerm("discounts.header")))
    assert(!mutator.matcher.isDefinedAt(parseTerm("discounts.map(process)")))
    assert(!mutator.matcher.isDefinedAt(parseTerm("discounts")))
  }
}
