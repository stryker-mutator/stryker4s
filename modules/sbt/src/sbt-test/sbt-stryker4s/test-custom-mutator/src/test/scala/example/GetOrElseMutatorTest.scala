package example

import scala.meta.*

/** Unit tests for [[GetOrElseMutator]], written first (TDD) before the mutator implementation. */
class GetOrElseMutatorTest extends munit.FunSuite {
  private val mutator = new GetOrElseMutator

  private def parseTerm(code: String): Term =
    code.parse[Term].get

  private def mutate(code: String) = {
    val term = parseTerm(code)
    assert(mutator.matcher.isDefinedAt(term), s"expected matcher to be defined at: $code")
    val Right(mutations) =
      mutator.matcher(term)(stryker4s.mutatorapi.PlaceableTree(term)): @unchecked
    mutations
  }

  test("GetOrElseMutator forces the default of a one-argument getOrElse") {
    val mutations = mutate("maybeQuantity.getOrElse(0)")

    assertEquals(mutations.length, 1)
    assertEquals(mutations.head.metadata.mutatorName, "GetOrElse")
    assertEquals(mutations.head.mutatedStatement.syntax, "0")
  }

  test("GetOrElseMutator forces the default of a two-argument Map getOrElse") {
    val mutations = mutate("quantities.getOrElse(key, 0)")

    assertEquals(mutations.head.mutatedStatement.syntax, "0")
  }

  test("GetOrElseMutator preserves a complex default expression") {
    val mutations = mutate("lookup(id).getOrElse(fallbackFor(id))")

    assertEquals(mutations.head.mutatedStatement.syntax, "fallbackFor(id)")
  }

  test("GetOrElseMutator handles the infix form") {
    val mutations = mutate("maybeQuantity getOrElse 0")

    assertEquals(mutations.head.mutatedStatement.syntax, "0")
  }

  test("GetOrElseMutator also covers getOrElseF") {
    val mutations = mutate("maybeQuantity.getOrElseF(IO.pure(0))")

    assertEquals(mutations.head.mutatedStatement.syntax, "IO.pure(0)")
  }

  test("GetOrElseMutator records the receiver-less default in its metadata") {
    val mutations = mutate("maybeQuantity.getOrElse(0)")

    assertEquals(mutations.head.metadata.original, "getOrElse")
    assertEquals(mutations.head.metadata.replacement, "default")
  }

  test("GetOrElseMutator does not match unrelated combinators") {
    assert(!mutator.matcher.isDefinedAt(parseTerm("maybeQuantity.orElse(other)")))
    assert(!mutator.matcher.isDefinedAt(parseTerm("maybeQuantity.map(identity)")))
  }

  test("GetOrElseMutator does not match getOrElse with unexpected arity") {
    assert(!mutator.matcher.isDefinedAt(parseTerm("quantities.getOrElse()")))
    assert(!mutator.matcher.isDefinedAt(parseTerm("quantities.getOrElse(a, b, c)")))
  }
}
