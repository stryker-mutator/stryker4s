package stryker4s.mutatorapi

/** Public extension point for third-party mutators.
  *
  * Implementations are registered by fully-qualified class name via the `customMutators` configuration option, and must
  * have a public no-argument constructor so Stryker4s can instantiate them reflectively.
  *
  * Example:
  * {{{
  * class MyCustomMutator extends CustomMutator {
  *   def matcher: MutationMatcher = {
  *     case term @ Term.ApplyInfix.After_4_6_0(_, op @ Term.Name("+"), _, _) =>
  *       placeableTree =>
  *         val mutated = term.copy(op = op.copy(value = "-"))
  *         val metadata = MutantMetadata("+", "-", "ArithmeticOperator", term.pos, None)
  *         Right(NonEmptyVector.one(MutatedCode(placeableTree.substitute(term, mutated), metadata)))
  *   }
  * }
  * }}}
  *
  * Two things are easy to get wrong, and neither shows up until a mutant fails to compile:
  *
  *   - A [[MutatedCode]] carries the whole placeable statement, not just the replacement sub-term. Build it with
  *     [[PlaceableTree.substitute]] rather than returning the replacement directly.
  *   - Stryker4s visits every node of the tree, so a matcher that accepts both `expr.foo(arg)` and its inner `expr.foo`
  *     selection emits two mutants for one combinator, the second of which leaves the argument list dangling. Keep
  *     applied and no-argument method names in disjoint sets.
  */
trait CustomMutator {

  /** The matcher this custom mutator contributes. It is combined with Stryker4s's built-in matchers (and any other
    * configured custom mutators) before mutants are collected.
    */
  def matcher: MutationMatcher
}
