package stryker4s.extension.mutationtype

import scala.meta.Term
import scala.meta.Term.*
import scala.meta.Type

/** Base trait for method calls with one or multiple argument(s)
  */
sealed trait ArgMethodExpression extends MethodExpression {
  def unapply(term: Term): Option[(Term, String => Term)] =
    term match {
      // foo.filter { (a,b) => a > b }
      case Apply.After_4_6_0(
            Select(_, Name(`methodName`)),
            ArgClause(Block(Function.Initial(_ :: _ :: _, _) :: Nil) :: Nil, _)
          ) =>
        None

      // foo.filter((a,b) => a > b)
      case Apply.After_4_6_0(Select(_, Name(`methodName`)), ArgClause(Function.Initial(_ :: _ :: _, _) :: Nil, _)) =>
        None

      // foo filter { (a,b) => a > b }
      case ApplyInfix.After_4_6_0(
            _,
            Name(`methodName`),
            Type.ArgClause(Nil),
            ArgClause(List(Block(Function.Initial(_ :: _ :: _, _) :: Nil)), None)
          ) =>
        None

      // foo filter((a,b) => a > b)
      case ApplyInfix.After_4_6_0(
            _,
            Name(`methodName`),
            Type.ArgClause(Nil),
            ArgClause(Function.Initial(_ :: _ :: _, _) :: Nil, _)
          ) =>
        None

      // foo.filter( a => a > 0 )
      case Apply.After_4_6_0(Select(q, Name(`methodName`)), ArgClause(arg :: Nil, clause)) =>
        Option((term, name => Apply.After_4_6_0(Term.Select(q, Name(name)), ArgClause(arg :: Nil, clause))))

      // foo filter( a => a > 0 )
      case ApplyInfix.After_4_6_0(q, Name(`methodName`), Type.ArgClause(Nil), ArgClause(arg :: Nil, clause)) =>
        Option(
          (term, name => ApplyInfix.After_4_6_0(q, Name(name), Type.ArgClause(Nil), ArgClause(arg :: Nil, clause)))
        )

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

case object TakeRight extends ArgMethodExpression {
  protected val methodName = "takeRight"
}

case object DropRight extends ArgMethodExpression {
  protected val methodName = "dropRight"
}

case object TakeWhile extends ArgMethodExpression {
  protected val methodName = "takeWhile"
}

case object DropWhile extends ArgMethodExpression {
  protected val methodName = "dropWhile"
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
