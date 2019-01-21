package stryker4s.extension.mutationtype

import scala.meta.Term
import scala.meta.Term.{Apply, ApplyInfix, Block, Function, Name, Select}

/**
  * Base trait for method calls with one or multiple argument(s)
  */
trait ArgMethodExpression extends MethodExpression {

  def unapply(term: Term): Option[(Term, String => Term)] = term match {

    // foo.filter { (a,b) => a > b }
    case Apply(Select(_, Name(`methodName`)), Block(Function(_ :: _ :: _, _) :: Nil) :: Nil) =>
      None

    // foo.filter((a,b) => a > b)
    case Apply(Select(_, Name(`methodName`)), Function(_ :: _ :: _, _) :: Nil) =>
      None

    // foo filter { (a,b) => a > b }
    case ApplyInfix(_, Name(`methodName`), Nil, Block(Function(_ :: _ :: _, _) :: Nil) :: Nil) =>
      None

    // foo filter((a,b) => a > b)
    case ApplyInfix(_, Name(`methodName`), Nil, Function(_ :: _ :: _, _) :: Nil) =>
      None

    // foo.filter( a => a > 0 )
    case Apply(Select(q, Name(`methodName`)), arg :: Nil) =>
      Option(term, name => Apply(Term.Select(q, Name(name)), arg :: Nil))

    // foo filter( a => a > 0 )
    case ApplyInfix(q, Name(`methodName`), Nil, arg :: Nil) =>
      Option(term, name => ApplyInfix(q, Name(name), Nil, arg :: Nil))

    case _ => None
  }

}

case object Filter extends ArgMethodExpression {
  protected val methodName = "filter"
}

case object FilterNot extends ArgMethodExpression {
  protected val methodName = "filterNot"
}

case object Exists extends ArgMethodExpression {
  protected val methodName = "exists"
}

case object Forall extends ArgMethodExpression {
  protected val methodName = "forall"
}

case object Take extends ArgMethodExpression {
  protected val methodName = "take"
}

case object Drop extends ArgMethodExpression {
  protected val methodName = "drop"
}

case object IndexOf extends ArgMethodExpression {
  protected val methodName = "indexOf"
}

case object LastIndexOf extends ArgMethodExpression {
  protected val methodName = "lastIndexOf"
}

case object MaxBy extends ArgMethodExpression {
  protected val methodName = "maxBy"
}

case object MinBy extends ArgMethodExpression {
  protected val methodName = "minBy"
}
