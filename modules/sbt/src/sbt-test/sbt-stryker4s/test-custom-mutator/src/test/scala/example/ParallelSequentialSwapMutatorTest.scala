package example

import scala.meta.*

/** Unit tests for [[ParallelSequentialSwapMutator]], written first (TDD) before the mutator
  * implementation.
  */
class ParallelSequentialSwapMutatorTest extends munit.FunSuite {
  private val mutator = new ParallelSequentialSwapMutator

  private def parseTerm(code: String): Term =
    code.parse[Term].get

  private def mutate(code: String) = {
    val term = parseTerm(code)
    assert(mutator.matcher.isDefinedAt(term), s"expected matcher to be defined at: $code")
    val Right(mutations) =
      mutator.matcher(term)(stryker4s.mutatorapi.PlaceableTree(term)): @unchecked
    mutations
  }

  test("ParallelSequentialSwapMutator makes traverse parallel") {
    val mutations = mutate("orders.traverse(process)")

    assertEquals(mutations.length, 1)
    assertEquals(mutations.head.metadata.mutatorName, "ParallelSequentialSwap")
    assertEquals(mutations.head.mutatedStatement.syntax, "orders.parTraverse(process)")
  }

  test("ParallelSequentialSwapMutator makes parTraverse sequential") {
    val mutations = mutate("orders.parTraverse(process)")

    assertEquals(mutations.head.mutatedStatement.syntax, "orders.traverse(process)")
  }

  test("ParallelSequentialSwapMutator swaps the result-discarding variants") {
    assertEquals(mutate("orders.traverse_(process)").head.mutatedStatement.syntax, "orders.parTraverse_(process)")
    assertEquals(mutate("orders.parTraverse_(process)").head.mutatedStatement.syntax, "orders.traverse_(process)")
  }

  test("ParallelSequentialSwapMutator swaps flatTraverse") {
    assertEquals(
      mutate("orders.flatTraverse(process)").head.mutatedStatement.syntax,
      "orders.parFlatTraverse(process)"
    )
  }

  test("ParallelSequentialSwapMutator swaps mapN") {
    assertEquals(mutate("(a, b).mapN(combine)").head.mutatedStatement.syntax, "(a, b).parMapN(combine)")
    assertEquals(mutate("(a, b).parMapN(combine)").head.mutatedStatement.syntax, "(a, b).mapN(combine)")
  }

  test("ParallelSequentialSwapMutator swaps the no-argument sequence combinators") {
    assertEquals(mutate("effects.sequence").head.mutatedStatement.syntax, "effects.parSequence")
    assertEquals(mutate("effects.parSequence").head.mutatedStatement.syntax, "effects.sequence")
    assertEquals(mutate("effects.sequence_").head.mutatedStatement.syntax, "effects.parSequence_")
  }

  test("ParallelSequentialSwapMutator records both directions in its metadata") {
    val toParallel = mutate("orders.traverse(process)").head.metadata
    assertEquals(toParallel.original, "traverse")
    assertEquals(toParallel.replacement, "parTraverse")
  }

  test("ParallelSequentialSwapMutator does not match an applied sequence combinator") {
    // `sequence` takes no arguments, so an applied one is something else entirely.
    assert(!mutator.matcher.isDefinedAt(parseTerm("effects.sequence(arg)")))
  }

  test("ParallelSequentialSwapMutator does not match a bare traverse selection") {
    // Guards against emitting a duplicate mutant for the inner Select of `xs.traverse(f)`.
    assert(!mutator.matcher.isDefinedAt(parseTerm("orders.traverse")))
  }

  test("ParallelSequentialSwapMutator does not match unrelated combinators") {
    assert(!mutator.matcher.isDefinedAt(parseTerm("orders.map(process)")))
    assert(!mutator.matcher.isDefinedAt(parseTerm("orders.parallelism")))
  }
}
