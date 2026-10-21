package stryker4s.plugin.api

/** Extension point for third-party mutators, exposed by a [[CustomMutatorPlugin]]. Needs a public no-argument
  * constructor. See the custom mutators guide in the docs for a full example.
  *
  * Two pitfalls only show up as mutants that fail to compile:
  *
  *   - A [[MutatedCode]] carries the whole placeable statement; build it with [[PlaceableTree.substitute]].
  *   - Every tree node is visited, so match `expr.foo(arg)` and `expr.foo` with disjoint method names.
  */
trait CustomMutator {

  /** Combined with the built-in matchers and other custom mutators before mutants are collected. */
  def matcher: MutationMatcher
}
