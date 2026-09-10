package example

import scala.meta.*

/** Unit tests for [[SleepRemovalMutator]], written first (TDD) before the mutator implementation. */
class SleepRemovalMutatorTest extends munit.FunSuite {
  private val mutator = new SleepRemovalMutator

  private def parseTerm(code: String): Term =
    code.parse[Term].get

  private def mutate(code: String) = {
    val term = parseTerm(code)
    assert(mutator.matcher.isDefinedAt(term), s"expected matcher to be defined at: $code")
    val Right(mutations) =
      mutator.matcher(term)(stryker4s.mutatorapi.PlaceableTree(term)): @unchecked
    mutations
  }

  test("SleepRemovalMutator turns IO.sleep into IO.unit") {
    val mutations = mutate("IO.sleep(backoff)")

    assertEquals(mutations.length, 1)
    assertEquals(mutations.head.metadata.mutatorName, "SleepRemoval")
    assertEquals(mutations.head.mutatedStatement.syntax, "IO.unit")
  }

  test("SleepRemovalMutator preserves the effect qualifier") {
    assertEquals(mutate("Temporal[F].sleep(backoff)").head.mutatedStatement.syntax, "Temporal[F].unit")
    assertEquals(mutate("cats.effect.IO.sleep(backoff)").head.mutatedStatement.syntax, "cats.effect.IO.unit")
  }

  test("SleepRemovalMutator removes delayBy, leaving the receiver") {
    assertEquals(mutate("publish.delayBy(backoff)").head.mutatedStatement.syntax, "publish")
  }

  test("SleepRemovalMutator removes andWait, leaving the receiver") {
    assertEquals(mutate("publish.andWait(backoff)").head.mutatedStatement.syntax, "publish")
  }

  test("SleepRemovalMutator records the removed delay in its metadata") {
    assertEquals(mutate("IO.sleep(backoff)").head.metadata.original, "sleep")
    assertEquals(mutate("publish.delayBy(backoff)").head.metadata.original, "delayBy")
  }

  test("SleepRemovalMutator does not match Thread.sleep") {
    // Thread has no `unit`, so this would only ever be a compile error.
    assert(!mutator.matcher.isDefinedAt(parseTerm("Thread.sleep(1000)")))
  }

  test("SleepRemovalMutator does not match sleep with unexpected arity") {
    assert(!mutator.matcher.isDefinedAt(parseTerm("IO.sleep()")))
    assert(!mutator.matcher.isDefinedAt(parseTerm("IO.sleep(a, b)")))
  }

  test("SleepRemovalMutator does not match unrelated combinators") {
    assert(!mutator.matcher.isDefinedAt(parseTerm("IO.pure(value)")))
    assert(!mutator.matcher.isDefinedAt(parseTerm("publish.timeout(backoff)")))
  }
}
