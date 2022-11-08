package stryker4jvm.extensions

import scala.meta.Tree

import stryker4jvm.extensions.mutationtype.SubstitutionMutation

/** Converts [[stryker4s.extension.mutationtype.SubstitutionMutation]] to a `scala.meta.Tree`
  *
  * {{{
  * import stryker4jvm.extension.ImplicitMutationConversion._
  * val gt: Tree = GreaterThan
  * }}}
  */
object ImplicitMutationConversion {
  implicit def mutationToTree[T <: Tree](mutation: SubstitutionMutation[T]): T = mutation.tree
}
