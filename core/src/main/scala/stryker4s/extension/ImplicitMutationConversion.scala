package stryker4s.extension

import scala.meta.Tree

import stryker4s.extension.mutationtype.SubstitutionMutation

/** Converts [[stryker4s.extension.mutationtype.SubstitutionMutation]] to a `scala.meta.Tree`
  *
  * {{{
  * import stryker4s.extension.ImplicitMutationConversion._
  * val gt: Tree = GreaterThan
  * }}}
  */
object ImplicitMutationConversion {
  implicit def mutationToTree[T <: Tree](mutation: SubstitutionMutation[T]): T = mutation.tree
}
