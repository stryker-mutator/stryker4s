package stryker4jvm.mutator.scala.extensions.mutationtype

import scala.meta.*

case object If {
  def unapply(ifStatement: Term.If): Option[Term] = Some(ifStatement.cond).filterNot(_.is[Lit.Boolean])
}

case object While {
  def unapply(whileStatement: Term.While): Option[Term] = Some(whileStatement.expr).filterNot(_.is[Lit.Boolean])
}

case object DoWhile {
  def unapply(doStatement: Term.Do): Option[Term] = Some(doStatement.expr).filterNot(_.is[Lit.Boolean])
}

case object ConditionalTrue extends ConditionalExpression {
  override val tree: Lit.Boolean = Lit.Boolean(true)
}

case object ConditionalFalse extends ConditionalExpression {
  override val tree: Lit.Boolean = Lit.Boolean(false)
}
