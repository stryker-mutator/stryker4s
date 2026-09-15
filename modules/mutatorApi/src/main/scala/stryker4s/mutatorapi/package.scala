package stryker4s

import cats.data.NonEmptyVector

import scala.meta.Tree

package object mutatorapi {

  /** All mutations found for a single [[PlaceableTree]].
    */
  type Mutations = NonEmptyVector[MutatedCode]

  /** A mutation that was found, but excluded (e.g. by user configuration or a `@SuppressWarnings` annotation).
    */
  type IgnoredMutation = (MutatedCode, IgnoredMutationReason)

  type IgnoredMutations = NonEmptyVector[IgnoredMutation]

  /** A `PartialFunction` that can match on a ScalaMeta `Tree` and, for a given [[PlaceableTree]], return either the
    * mutations found (`Right`) or the reason they were ignored (`Left`).
    *
    * This is the type both Stryker4s's built-in mutators and third-party [[CustomMutator]]s implement.
    */
  type MutationMatcher = PartialFunction[Tree, PlaceableTree => Either[IgnoredMutations, Mutations]]

}
