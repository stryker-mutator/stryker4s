package stryker4s.mutants

import cats.data.{NonEmptyList, NonEmptyVector}
import stryker4s.model.MutantWithId

import scala.meta.Term

package object tree {

  // Re-exported from the public `stryker4s-plugin-api` module (see `stryker4s.model.package` for the same pattern)
  type Mutations = stryker4s.plugin.api.Mutations
  type IgnoredMutation = stryker4s.plugin.api.IgnoredMutation
  type IgnoredMutations = stryker4s.plugin.api.IgnoredMutations

  type MutantsWithId = NonEmptyVector[MutantWithId]

  type MutantCoverageTermFn = (NonEmptyList[Int]) => Term

}
