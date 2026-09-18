package example

import scala.meta.*

/** Unit tests for [[CollectionOrderMutator]]'s matcher logic, written first (TDD) before the mutator implementation.
  */
class CollectionOrderMutatorTest extends munit.FunSuite {
  private val mutator = new CollectionOrderMutator

  private def parseTerm(code: String): Term =
    code.parse[Term].get

  private def mutate(code: String): String = {
    val term = parseTerm(code)
    val Right(mutations) = mutator.matcher(term)(stryker4s.mutatorapi.PlaceableTree(term)): @unchecked
    assertEquals(mutations.length, 1)
    assertEquals(mutations.head.metadata.mutatorName, "CollectionOrder")
    mutations.head.mutatedStatement.syntax
  }

  test("drops .sorted, leaving the receiver in its original order") {
    assertEquals(mutate("discounts.sorted"), "discounts")
  }

  test("drops .reverse") {
    assertEquals(mutate("discounts.reverse"), "discounts")
  }

  test("drops .distinct") {
    assertEquals(mutate("discounts.distinct"), "discounts")
  }

  test("drops .sortBy(f)") {
    assertEquals(mutate("discounts.sortBy(magnitude)"), "discounts")
  }

  test("drops .sortWith(f)") {
    assertEquals(mutate("discounts.sortWith(byRank)"), "discounts")
  }

  test("drops .distinctBy(f)") {
    assertEquals(mutate("discounts.distinctBy(code)"), "discounts")
  }

  test("keeps the statement surrounding the matched sub-term") {
    val statement = parseTerm("discounts.sorted.headOption")
    val term = statement.collect { case t: Term if t.syntax == "discounts.sorted" => t }.head

    val Right(mutations) = mutator.matcher(term)(stryker4s.mutatorapi.PlaceableTree(statement)): @unchecked
    assertEquals(mutations.head.mutatedStatement.syntax, "discounts.headOption")
  }

  test("does not match the selection an ordering combinator is applied to") {
    // `discounts.sortBy` on its own would drop the selection and leave `discounts(magnitude)`.
    val statement = parseTerm("discounts.sortBy(magnitude)")
    val select = statement.collect { case t: Term.Select => t }.head

    assert(!mutator.matcher.isDefinedAt(select))
  }

  test("does not match a no-argument combinator that is being applied") {
    // `sorted` takes no arguments, so `discounts.sorted(ordering)` is passing an implicit
    // explicitly; dropping the selection would leave `discounts(ordering)`.
    val statement = parseTerm("discounts.sorted(ordering)")
    val select = statement.collect { case t: Term.Select => t }.head

    assert(!mutator.matcher.isDefinedAt(select))
    assert(!mutator.matcher.isDefinedAt(statement))
  }

  test("does not match unrelated combinators") {
    assert(!mutator.matcher.isDefinedAt(parseTerm("discounts.map(process)")))
    assert(!mutator.matcher.isDefinedAt(parseTerm("discounts.sortedness")))
    assert(!mutator.matcher.isDefinedAt(parseTerm("discounts")))
  }
}
