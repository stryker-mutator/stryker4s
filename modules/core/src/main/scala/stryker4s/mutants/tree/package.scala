package stryker4s.mutants

import cats.data.{NonEmptyList, NonEmptyVector}
import stryker4s.model.MutantWithId

import scala.meta.Term

package object tree {

  // Re-exported from the public `stryker4s-mutator-api` module (see `stryker4s.model.package` for the same pattern)
  type Mutations = stryker4s.mutatorapi.Mutations
  type IgnoredMutation = stryker4s.mutatorapi.IgnoredMutation
  type IgnoredMutations = stryker4s.mutatorapi.IgnoredMutations

  type MutantsWithId = NonEmptyVector[MutantWithId]

  type MutantCoverageTermFn = (NonEmptyList[Int]) => Term

}
