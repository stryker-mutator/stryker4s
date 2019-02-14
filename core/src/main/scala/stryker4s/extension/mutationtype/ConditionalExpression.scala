package stryker4s.extension.mutationtype

import scala.meta.{Lit, Term}

case object IfStatement {
  def unapply(tree: Term.If): Option[(Term, Seq[IfStatementMutation])] = {
    Some(
      tree,
      Seq(
        IfStatementMutation(tree.copy(cond = Lit.Boolean(true))),
        IfStatementMutation(tree.copy(cond = Lit.Boolean(false))),
      )
    )
  }
  case class IfStatementMutation(tree: Term.If) extends ConditionalExpression[Term.If]
}
