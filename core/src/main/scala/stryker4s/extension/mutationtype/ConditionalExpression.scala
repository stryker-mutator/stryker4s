package stryker4s.extension.mutationtype

import scala.meta.{Lit, Term}

object IfStatement {
  def unapply(tree: Term.If): Option[(Term, Seq[IfStatement])] = {
    Some(
      tree,
      Seq(
        new IfStatement(tree.copy(cond = Lit.Boolean(true))),
        new IfStatement(tree.copy(cond = Lit.Boolean(false))),
      )
    )
  }
}
class IfStatement(val tree: Term.If) extends ConditionalExpression[Term.If]
