package stryker4s.extensions

import stryker4s.extensions.mutationtypes.Mutation

import scala.meta.Tree

/**
  * Converts [[stryker4s.extensions.mutationtypes.Mutation]] to a `scala.meta.Tree`
  *
  * {{{
  *  import stryker4s.extensions.ImplicitMutationConversion._
  *  val gt: Tree = GreaterThan
  * }}}
  */
object ImplicitMutationConversion {
  // Import implicitConversions to disable compiler warning
  import scala.language.implicitConversions

  implicit def mutationToTree[T <: Tree](mutation: Mutation[T]): T = mutation.tree
}
