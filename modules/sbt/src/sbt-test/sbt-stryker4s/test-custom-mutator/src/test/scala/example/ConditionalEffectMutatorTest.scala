package example

import scala.meta.*

/** Unit tests for [[ConditionalEffectMutator]]'s matcher logic, written first (TDD) before the
  * mutator implementation. These cover cats-effect conditional-effect helpers that encode
  * side-effecting boolean guards.
  */
class ConditionalEffectMutatorTest extends munit.FunSuite {
  private val mutator = new ConditionalEffectMutator

  private def parseTerm(code: String): Term =
    code.parse[Term].get

  private def mutatedSyntax(code: String): String = {
    val term = parseTerm(code)

    assert(mutator.matcher.isDefinedAt(term))

    val Right(mutations) = mutator.matcher(term)(stryker4s.mutatorapi.PlaceableTree(term)): @unchecked
    assertEquals(mutations.length, 1)
    assertEquals(mutations.head.metadata.mutatorName, "ConditionalEffect")
    mutations.head.mutatedStatement.syntax
  }

  test("flips IO.whenA(cond)(effect) to IO.unlessA(cond)(effect)") {
    assertEquals(mutatedSyntax("IO.whenA(flag)(publish)"), "IO.unlessA(flag)(publish)")
  }

  test("flips IO.unlessA(cond)(effect) to IO.whenA(cond)(effect)") {
    assertEquals(mutatedSyntax("IO.unlessA(flag)(publish)"), "IO.whenA(flag)(publish)")
  }

  test("flips IO.raiseWhen(cond)(error) to IO.raiseUnless(cond)(error)") {
    assertEquals(mutatedSyntax("IO.raiseWhen(flag)(error)"), "IO.raiseUnless(flag)(error)")
  }

  test("flips IO.raiseUnless(cond)(error) to IO.raiseWhen(cond)(error)") {
    assertEquals(mutatedSyntax("IO.raiseUnless(flag)(error)"), "IO.raiseWhen(flag)(error)")
  }

  test("preserves complex receiver and argument expressions") {
    assertEquals(
      mutatedSyntax("cats.effect.IO.whenA(quantity > 10)(publish(quantity))"),
      "cats.effect.IO.unlessA(quantity > 10)(publish(quantity))"
    )
  }

  test("does not match unrelated method calls") {
    assert(!mutator.matcher.isDefinedAt(parseTerm("IO.delay(publish)")))
    assert(!mutator.matcher.isDefinedAt(parseTerm("someEffect.whenA(flag)")))
  }

  test("does not match a single-argument conditional helper call") {
    assert(!mutator.matcher.isDefinedAt(parseTerm("IO.whenA(flag)")))
  }
}
