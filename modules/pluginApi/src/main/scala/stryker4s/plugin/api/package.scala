package stryker4s.plugin

import cats.data.NonEmptyVector

import scala.meta.Tree

package object api {

  /** All mutations found for a single [[PlaceableTree]].
    */
  type Mutations = NonEmptyVector[MutatedCode]

  /** A mutation that was found, but excluded (e.g. by user configuration or a `@SuppressWarnings` annotation).
    */
  type IgnoredMutation = (MutatedCode, IgnoredMutationReason)

  type IgnoredMutations = NonEmptyVector[IgnoredMutation]

  /** Matches a `Tree` and, for a [[PlaceableTree]], returns the mutations found or the reason they were ignored. */
  type MutationMatcher = PartialFunction[Tree, PlaceableTree => Either[IgnoredMutations, Mutations]]

}
