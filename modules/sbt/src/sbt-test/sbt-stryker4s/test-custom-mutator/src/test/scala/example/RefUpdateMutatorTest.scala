package example

import scala.meta.*

/** Unit tests for [[RefUpdateMutator]], written first (TDD) before the mutator implementation. */
class RefUpdateMutatorTest extends munit.FunSuite {
  private val mutator = new RefUpdateMutator

  private def parseTerm(code: String): Term =
    code.parse[Term].get

  private def mutate(code: String) = {
    val term = parseTerm(code)
    assert(mutator.matcher.isDefinedAt(term), s"expected matcher to be defined at: $code")
    val Right(mutations) =
      mutator.matcher(term)(stryker4s.mutatorapi.PlaceableTree(term)): @unchecked
    mutations
  }

  test("RefUpdateMutator neutralises update with identity") {
    val mutations = mutate("counter.update(_ + 1)")

    assertEquals(mutations.length, 1)
    assertEquals(mutations.head.metadata.mutatorName, "RefUpdate")
    assertEquals(mutations.head.mutatedStatement.syntax, "counter.update(identity)")
  }

  test("RefUpdateMutator neutralises updateAndGet and getAndUpdate") {
    assertEquals(mutate("counter.updateAndGet(_ + 1)").head.mutatedStatement.syntax, "counter.updateAndGet(identity)")
    assertEquals(mutate("counter.getAndUpdate(_ + 1)").head.mutatedStatement.syntax, "counter.getAndUpdate(identity)")
  }

  test("RefUpdateMutator neutralises a named function argument") {
    val mutations = mutate("counter.update(increment)")

    assertEquals(mutations.head.mutatedStatement.syntax, "counter.update(identity)")
  }

  test("RefUpdateMutator preserves a complex receiver") {
    val mutations = mutate("state.counters(key).update(_ + 1)")

    assertEquals(mutations.head.mutatedStatement.syntax, "state.counters(key).update(identity)")
  }

  test("RefUpdateMutator records the neutralised update in its metadata") {
    val metadata = mutate("counter.update(_ + 1)").head.metadata

    assertEquals(metadata.original, "update")
    assertEquals(metadata.replacement, "identity")
  }

  test("RefUpdateMutator does not match an update that is already identity") {
    // Would be an equivalent mutant, so it is skipped rather than reported as survived.
    assert(!mutator.matcher.isDefinedAt(parseTerm("counter.update(identity)")))
  }

  test("RefUpdateMutator does not match a two-argument update") {
    // e.g. mutable sequence `xs.update(index, value)`, which has no identity form.
    assert(!mutator.matcher.isDefinedAt(parseTerm("buffer.update(0, value)")))
  }

  test("RefUpdateMutator does not match unrelated combinators") {
    assert(!mutator.matcher.isDefinedAt(parseTerm("counter.modify(_ + 1)")))
    assert(!mutator.matcher.isDefinedAt(parseTerm("counter.set(1)")))
  }
}
