package stryker4jvm.mutator.scala.extensions

import scala.meta.{Term, Tree}

import stryker4jvm.mutator.scala.extensions.mutationtype.SubstitutionMutation
import stryker4jvm.mutator.scala.ScalaAST
import scala.meta.Source

/** Converts [[stryker4jvm.mutator.scala.extensions.mutationtype.SubstitutionMutation]] to a `scala.meta.Tree`
  *
  * {{{
  * import stryker4jvm.extension.ImplicitMutationConversion._
  * val gt: Tree = GreaterThan
  * }}}
  */
object ImplicitMutationConversion {
  implicit def mutationToTree[T <: Tree](mutation: SubstitutionMutation[T]): T = mutation.tree

  implicit def ASTToTerm(ast: ScalaAST): Term = ast.term
  implicit def TermToAST(term: Term): ScalaAST = new ScalaAST(term = term)

  implicit def ASTToSource(ast: ScalaAST): Source = ast.source
  implicit def SourceToAST(source: Source): ScalaAST = new ScalaAST(source = source)

  implicit def ASTToTree(ast: ScalaAST): Tree = ast.tree
  implicit def TreeToAST(tree: Tree): ScalaAST = new ScalaAST(tree = tree)
}
