package stryker4s.extension.mutationtype

import scala.meta._

case object If {

  def unapply(term: Term): Option[Term] =
    term.parent collect {
      case Term.If(cond, _, _) if cond == term => term
    } filterNot (_.is[Lit.Boolean])
}

case object IfTrue extends ConditionalExpression {
  override  val tree: Lit.Boolean = Lit.Boolean(true)
}
case object IfFalse extends ConditionalExpression {
  override  val tree: Lit.Boolean = Lit.Boolean(false)
}
