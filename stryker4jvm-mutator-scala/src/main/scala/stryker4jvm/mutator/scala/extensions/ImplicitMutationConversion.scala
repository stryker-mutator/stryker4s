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
  *
  * Also converts ScalaASTs to a `scala.meta.Term`, a `scala.meta.Tree` or a `scala.meta.Source`
  */
object ImplicitMutationConversion {
  implicit def mutationToTree[T <: Tree](mutation: SubstitutionMutation[T]): T = mutation.tree

  implicit def ASTToTerm(ast: ScalaAST): Term = ast.value.asInstanceOf[Term]
  implicit def TermToAST(term: Term): ScalaAST = new ScalaAST(value = term)

  implicit def ASTToSource(ast: ScalaAST): Source = ast.value.asInstanceOf[Source]
  implicit def SourceToAST(source: Source): ScalaAST = new ScalaAST(value = source)

  implicit def ASTToTree(ast: ScalaAST): Tree = ast.value
  implicit def TreeToAST(tree: Tree): ScalaAST = new ScalaAST(value = tree)
}
