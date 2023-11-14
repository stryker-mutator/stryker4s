package stryker4s.mutation

import cats.syntax.option.*

import scala.meta.*

case object If {
  def unapply(ifStatement: Term.If): Option[Term] =
    ifStatement.cond.some.filterNot(_.is[Lit.Boolean])
}

case object While {
  def unapply(whileStatement: Term.While): Option[Term] =
    whileStatement.expr.some.filterNot(_.is[Lit.Boolean])
}

case object DoWhile {
  def unapply(doStatement: Term.Do): Option[Term] =
    doStatement.expr.some.filterNot(_.is[Lit.Boolean])
}

case object ConditionalTrue extends ConditionalExpression {
  override val tree: Lit.Boolean = Lit.Boolean(true)
}

case object ConditionalFalse extends ConditionalExpression {
  override val tree: Lit.Boolean = Lit.Boolean(false)
}
