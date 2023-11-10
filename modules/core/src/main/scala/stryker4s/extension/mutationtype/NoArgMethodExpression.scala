package stryker4s.extension.mutationtype

import cats.syntax.option.*

import scala.meta.Term
import scala.meta.Term.{Name, Select}

/** Base method for methods call without arguments
  */
sealed trait NoArgMethodExpression extends MethodExpression {
  def unapply(term: Term): Option[(Term, String => Term)] =
    term match {
      // foo.filter or foo filter
      case Select(q, Name(`methodName`)) => Option((term, name => Select(q, Name(name))))
      case _                             => none
    }
}

case object IsEmpty extends NoArgMethodExpression {
  protected val methodName = "isEmpty"
}

case object NonEmpty extends NoArgMethodExpression {
  protected val methodName = "nonEmpty"
}

case object Max extends NoArgMethodExpression {
  protected val methodName = "max"
}

case object Min extends NoArgMethodExpression {
  protected val methodName = "min"
}
