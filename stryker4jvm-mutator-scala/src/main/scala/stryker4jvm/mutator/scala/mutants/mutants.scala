package stryker4jvm.mutator.scala

import cats.data.{NonEmptyList, NonEmptyVector}
import stryker4jvm.core.model.{IgnoredMutationReason, MutantWithId, MutatedCode}
import stryker4jvm.mutator.scala.model.PlaceableTree

import scala.meta.{Term, Tree}

package object mutants {

  type Mutations = NonEmptyVector[MutatedCode[Term]]

  type IgnoredMutation = (MutatedCode[Term], IgnoredMutationReason)
  type IgnoredMutations = NonEmptyVector[(MutatedCode[Term], IgnoredMutationReason)]

  type MutantsWithId = NonEmptyVector[MutantWithId[Term]]

  type MutationMatcher = PartialFunction[Tree, PlaceableTree => Mutations]

  type DefaultMutationCondition = (NonEmptyList[Int]) => Term

}
