package stryker4jvm.mutants

import cats.data.{NonEmptyList, NonEmptyVector}
import stryker4jvm.core.model.{MutantWithId, MutatedCode}
import stryker4jvm.model.{IgnoredMutationReason, PlaceableTree}

import scala.meta.{Term, Tree}

import stryker4jvm.mutants.language.ScalaAST

package object tree {

  type Mutations = NonEmptyVector[MutatedCode[Term]]

  type IgnoredMutation = (MutatedCode[Term], IgnoredMutationReason)
  type IgnoredMutations = NonEmptyVector[(MutatedCode[Term], IgnoredMutationReason)]

  type MutantsWithId = NonEmptyVector[MutantWithId[Term]]

  type MutationMatcher = PartialFunction[Tree, PlaceableTree => Mutations]

  type DefaultMutationCondition = (NonEmptyList[Int]) => Term

}
