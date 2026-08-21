package stryker4s.mutatorapi

/** Public extension point for third-party mutators.
  *
  * Implementations are registered by fully-qualified class name via the `customMutators` configuration option, and
  * must have a public no-argument constructor so Stryker4s can instantiate them reflectively.
  *
  * Example:
  * {{{
  * class MyCustomMutator extends CustomMutator {
  *   def matcher: MutationMatcher = {
  *     case ... => placeableTree => ...
  *   }
  * }
  * }}}
  */
trait CustomMutator {

  /** The matcher this custom mutator contributes. It is combined with Stryker4s's built-in matchers (and any other
    * configured custom mutators) before mutants are collected.
    */
  def matcher: MutationMatcher
}
