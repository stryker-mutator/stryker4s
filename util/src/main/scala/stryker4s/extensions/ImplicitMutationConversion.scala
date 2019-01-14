package stryker4s.extensions

import stryker4s.extensions.mutationtypes.SubstitutionMutation

import scala.language.implicitConversions
import scala.meta.Tree

/**
  * Converts [[stryker4s.extensions.mutationtypes.SubstitutionMutation]] to a `scala.meta.Tree`
  *
  * {{{
  *  import stryker4s.extensions.ImplicitMutationConversion._
  *  val gt: Tree = GreaterThan
  * }}}
  */
object ImplicitMutationConversion {

  implicit def mutationToTree[T <: Tree](mutation: SubstitutionMutation[T]): T = mutation.tree
}
