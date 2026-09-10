package example

import scala.meta.*

/** Unit tests for the cats-effect timeout mutator, written first (TDD) before the mutator implementation. These cover
  * the `timeout`, `timeoutTo` and `timeoutAndForget` combinators.
  */
class TimeoutMutatorTest extends munit.FunSuite {
  private val mutator = new TimeoutRemovalMutator

  private def parseTerm(code: String): Term =
    code.parse[Term].get

  private def mutate(code: String) = {
    val term = parseTerm(code)
    assert(mutator.matcher.isDefinedAt(term), s"expected matcher to be defined at: $code")
    val Right(mutations) =
      mutator.matcher(term)(stryker4s.mutatorapi.PlaceableTree(term)): @unchecked
    mutations
  }

  test("TimeoutRemovalMutator removes .timeout, keeping the receiver") {
    val mutations = mutate("fetch.timeout(5.seconds)")

    assertEquals(mutations.length, 1)
    assertEquals(mutations.head.metadata.mutatorName, "TimeoutRemoval")
    assertEquals(mutations.head.mutatedStatement.syntax, "fetch")
  }

  test("TimeoutRemovalMutator removes .timeoutTo, discarding the fallback") {
    val mutations = mutate("fetch.timeoutTo(5.seconds, IO.pure(0))")

    assertEquals(mutations.head.metadata.mutatorName, "TimeoutRemoval")
    assertEquals(mutations.head.mutatedStatement.syntax, "fetch")
  }

  test("TimeoutRemovalMutator removes .timeoutAndForget") {
    val mutations = mutate("fetch.timeoutAndForget(5.seconds)")

    assertEquals(mutations.head.mutatedStatement.syntax, "fetch")
  }

  test("TimeoutRemovalMutator preserves a complex receiver expression") {
    val mutations = mutate("client.get(url).flatMap(parse).timeout(5.seconds)")

    assertEquals(mutations.head.mutatedStatement.syntax, "client.get(url).flatMap(parse)")
  }

  test("TimeoutRemovalMutator records the removed combinator in its metadata") {
    val mutations = mutate("fetch.timeoutTo(5.seconds, IO.pure(0))")

    assertEquals(mutations.head.metadata.original, "timeoutTo")
  }

  test("TimeoutRemovalMutator does not match unrelated combinators") {
    assert(!mutator.matcher.isDefinedAt(parseTerm("fetch.orElse(fallback)")))
    assert(!mutator.matcher.isDefinedAt(parseTerm("fetch.handleErrorWith(recover)")))
  }

  test("TimeoutRemovalMutator does not match timeout combinators with unexpected arity") {
    assert(!mutator.matcher.isDefinedAt(parseTerm("fetch.timeout(5.seconds, extra)")))
    assert(!mutator.matcher.isDefinedAt(parseTerm("fetch.timeoutTo(5.seconds)")))
  }
}
