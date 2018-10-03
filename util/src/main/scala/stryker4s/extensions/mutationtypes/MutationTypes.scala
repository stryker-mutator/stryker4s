package stryker4s.extensions.mutationtypes

import scala.meta.Term.{Apply, ApplyInfix, Block, Function, Name, Select}
import scala.meta.contrib._
import scala.meta.{Lit, Term, Tree}

/**
  * Base trait for mutations. Mutations can be used to pattern match on (see MutantMatcher).
  */
sealed trait Mutation[T <: Tree]

/**
  * Base trait for substitution mutation
  *
  * Can implicitly be converted to the appropriate `scala.meta.Tree` by importing [[stryker4s.extensions.ImplicitMutationConversion]]
  *
  * @tparam T Has to be a subtype of Tree.
  *           This is so that the tree value and unapply methods return the appropriate type.
  *           E.G. A False is of type `scala.meta.Lit.Boolean` instead of a standard `scala.meta.Term`
  */
trait SubstitutionMutation[T <: Tree] extends Mutation[T] {
  val tree: T
  def unapply(arg: T): Option[T] =
    Some(arg)
      .filter(_.isEqual(tree))
      .filterNot {
        case name: Term.Name => name.isDefinition
        case _               => false
      }
}

trait BinaryOperator extends SubstitutionMutation[Term.Name]

trait BooleanSubstitution extends SubstitutionMutation[Lit.Boolean]

trait LogicalOperator extends SubstitutionMutation[Term.Name]

/** T &lt;: Term because it can be either a `Lit.String` or `Term.Interpolation`
  */
trait StringMutator[T <: Term] extends SubstitutionMutation[T]

/**
  * Base trait for method mutation
  */
trait MethodMutator extends Mutation[Term] {

  /**
    * Method to be replaced or to replace
    */
  protected val methodName: String

  def apply(f: String => Term): Term = f(methodName)

  def unapply(term: Term): Option[(Term, String => Term)]

}

/**
  * Base trait for method calls with one argument
  */
trait OneArgMethodMutator extends MethodMutator {

  def unapply(term: Term): Option[(Term, String => Term)] = term match {

    // foo.filter { (a,b) => a > b }
    case Apply(Select(q, Name(`methodName`)), Block(Function(_ :: _ :: _, _) :: Nil) :: Nil) =>
      None

    // foo.filter((a,b) => a > b)
    case Apply(Select(q, Name(`methodName`)), Function(_ :: _ :: _, _) :: Nil) =>
      None

    // foo filter { (a,b) => a > b }
    case ApplyInfix(q, Name(`methodName`), Nil, Block(Function(_ :: _ :: _, _) :: Nil) :: Nil) =>
      None

    // foo filter((a,b) => a > b)
    case ApplyInfix(q, Name(`methodName`), Nil, Function(_ :: _ :: _, _) :: Nil) =>
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

/**
  * Base method for methods call without arguments
  */
trait NonArgsMethodMutator extends MethodMutator {

  def unapply(term: Term): Option[(Term, String => Term)] = term match {
    // foo.filter or foo filter
    case Select(q, Name(`methodName`)) => Option(term, name => Select(q, Name(name)))
    case _                             => None
  }

}
