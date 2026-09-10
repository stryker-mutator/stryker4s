package example

import scala.meta.*

/** Unit tests for [[ErrorHandlingMutator]]'s matcher logic, written first (TDD) before the mutator implementation.
  * These test the `MutationMatcher` in isolation (parsing small snippets with scalameta, invoking the matcher directly)
  * rather than running a full mutation-testing pass, mirroring the style of `stryker4s.mutatorapi.CustomMutatorTest`.
  */
class ErrorHandlingMutatorTest extends munit.FunSuite {
  private val mutator = new ErrorHandlingMutator

  private def parseTerm(code: String): Term =
    code.parse[Term].get

  test("matches .attempt and drops it, leaving the receiver expression") {
    val term = parseTerm("someEffect.attempt")

    assert(mutator.matcher.isDefinedAt(term))

    val Right(mutations) = mutator.matcher(term)(stryker4s.mutatorapi.PlaceableTree(term)): @unchecked
    assertEquals(mutations.length, 1)
    assertEquals(mutations.head.metadata.mutatorName, "ErrorHandling")
    assertEquals(mutations.head.mutatedStatement.syntax, "someEffect")
  }

  test("matches .handleErrorWith(f) and drops it, leaving the receiver expression") {
    val term = parseTerm("someEffect.handleErrorWith(recover)")

    assert(mutator.matcher.isDefinedAt(term))

    val Right(mutations) = mutator.matcher(term)(stryker4s.mutatorapi.PlaceableTree(term)): @unchecked
    assertEquals(mutations.length, 1)
    assertEquals(mutations.head.mutatedStatement.syntax, "someEffect")
  }

  test("matches .handleError(f) and drops it, leaving the receiver expression") {
    val term = parseTerm("someEffect.handleError(recover)")

    assert(mutator.matcher.isDefinedAt(term))

    val Right(mutations) = mutator.matcher(term)(stryker4s.mutatorapi.PlaceableTree(term)): @unchecked
    assertEquals(mutations.head.mutatedStatement.syntax, "someEffect")
  }

  test("matches .recover(pf) and drops it, leaving the receiver expression") {
    val term = parseTerm("someEffect.recover(pf)")

    assert(mutator.matcher.isDefinedAt(term))

    val Right(mutations) = mutator.matcher(term)(stryker4s.mutatorapi.PlaceableTree(term)): @unchecked
    assertEquals(mutations.head.mutatedStatement.syntax, "someEffect")
  }

  test("matches .recoverWith(pf) and drops it, leaving the receiver expression") {
    val term = parseTerm("someEffect.recoverWith(pf)")

    assert(mutator.matcher.isDefinedAt(term))

    val Right(mutations) = mutator.matcher(term)(stryker4s.mutatorapi.PlaceableTree(term)): @unchecked
    assertEquals(mutations.head.mutatedStatement.syntax, "someEffect")
  }

  test("matches .rethrow and drops it, leaving the receiver expression") {
    val term = parseTerm("someEffect.rethrow")

    assert(mutator.matcher.isDefinedAt(term))

    val Right(mutations) = mutator.matcher(term)(stryker4s.mutatorapi.PlaceableTree(term)): @unchecked
    assertEquals(mutations.head.mutatedStatement.syntax, "someEffect")
  }

  test("does not match unrelated method calls") {
    assert(!mutator.matcher.isDefinedAt(parseTerm("someEffect.map(f)")))
    assert(!mutator.matcher.isDefinedAt(parseTerm("someEffect.flatMap(f)")))
  }

  test("does not match a bare identifier") {
    assert(!mutator.matcher.isDefinedAt(parseTerm("someEffect")))
  }

  test("does not match the bare selection of an applied combinator") {
    // Guards against emitting a duplicate mutant for the inner Select of `effect.handleErrorWith(f)`.
    // That mutant would drop only the selection and leave the argument list dangling, as in
    // `IO(a / b)(_ => IO.pure(0))`, which cannot compile.
    assert(!mutator.matcher.isDefinedAt(parseTerm("someEffect.handleErrorWith")))
    assert(!mutator.matcher.isDefinedAt(parseTerm("someEffect.handleError")))
    assert(!mutator.matcher.isDefinedAt(parseTerm("someEffect.recover")))
    assert(!mutator.matcher.isDefinedAt(parseTerm("someEffect.recoverWith")))
  }

  test("does not match an applied no-argument combinator") {
    // `attempt` and `rethrow` take no arguments, so an applied one is something else entirely.
    assert(!mutator.matcher.isDefinedAt(parseTerm("someEffect.attempt(arg)")))
    assert(!mutator.matcher.isDefinedAt(parseTerm("someEffect.rethrow(arg)")))
  }

  test("produces exactly one mutant for a whole applied combinator, keeping surrounding context") {
    val statement = parseTerm("IO(a / b).handleErrorWith(_ => IO.pure(0)).map(double)")
    val term = statement.collect { case t: Term if t.syntax == "IO(a / b).handleErrorWith(_ => IO.pure(0))" => t }.head

    val Right(mutations) =
      mutator.matcher(term)(stryker4s.mutatorapi.PlaceableTree(statement)): @unchecked
    assertEquals(mutations.length, 1)
    assertEquals(mutations.head.mutatedStatement.syntax, "IO(a / b).map(double)")
  }
}
