package stryker4s.extension.mutationtype

import scala.meta.Term
import stryker4s.extension.TreeExtensions.IsConditionOfIf

case object IfStatementCondition {

  def unapply(term: Term): Option[(Term, Seq[BooleanLiteral])] = {
    if (term.isConditionOfIf)
      Some((term, Seq(True, False)))
    else None
  }
}
