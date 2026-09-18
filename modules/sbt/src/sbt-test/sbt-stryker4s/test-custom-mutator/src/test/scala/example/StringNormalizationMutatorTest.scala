package example

import scala.meta.*

/** Unit tests for [[StringNormalizationMutator]]'s matcher logic, written first (TDD) before the mutator
  * implementation.
  */
class StringNormalizationMutatorTest extends munit.FunSuite {
  private val mutator = new StringNormalizationMutator

  private def parseTerm(code: String): Term =
    code.parse[Term].get

  private def mutate(code: String): String = {
    val term = parseTerm(code)
    val Right(mutations) = mutator.matcher(term)(stryker4s.mutatorapi.PlaceableTree(term)): @unchecked
    assertEquals(mutations.length, 1)
    assertEquals(mutations.head.metadata.mutatorName, "StringNormalization")
    mutations.head.mutatedStatement.syntax
  }

  test("drops .trim, leaving the unnormalized receiver") {
    assertEquals(mutate("code.trim"), "code")
  }

  test("drops .strip") {
    assertEquals(mutate("code.strip"), "code")
  }

  test("drops .toLowerCase") {
    assertEquals(mutate("code.toLowerCase"), "code")
  }

  test("drops .toUpperCase") {
    assertEquals(mutate("code.toUpperCase"), "code")
  }

  test("drops .capitalize") {
    assertEquals(mutate("code.capitalize"), "code")
  }

  test("drops .stripPrefix(p)") {
    assertEquals(mutate("code.stripPrefix(prefix)"), "code")
  }

  test("drops .stripSuffix(s)") {
    assertEquals(mutate("code.stripSuffix(suffix)"), "code")
  }

  test("keeps the statement surrounding the matched sub-term") {
    val statement = parseTerm("code.trim.toUpperCase")
    val term = statement.collect { case t: Term if t.syntax == "code.trim" => t }.head

    val Right(mutations) = mutator.matcher(term)(stryker4s.mutatorapi.PlaceableTree(statement)): @unchecked
    assertEquals(mutations.head.mutatedStatement.syntax, "code.toUpperCase")
  }

  test("does not match the selection a stripping combinator is applied to") {
    val statement = parseTerm("code.stripPrefix(prefix)")
    val select = statement.collect { case t: Term.Select => t }.head

    assert(!mutator.matcher.isDefinedAt(select))
  }

  test("does not match a no-argument combinator that is being applied") {
    // `toLowerCase` also has a `Locale` overload; dropping the selection of `code.toLowerCase(locale)`
    // would leave `code(locale)`.
    val statement = parseTerm("code.toLowerCase(locale)")
    val select = statement.collect { case t: Term.Select => t }.head

    assert(!mutator.matcher.isDefinedAt(select))
    assert(!mutator.matcher.isDefinedAt(statement))
  }

  test("does not match unrelated combinators") {
    assert(!mutator.matcher.isDefinedAt(parseTerm("code.length")))
    assert(!mutator.matcher.isDefinedAt(parseTerm("code.trimmed")))
    assert(!mutator.matcher.isDefinedAt(parseTerm("code")))
  }
}
