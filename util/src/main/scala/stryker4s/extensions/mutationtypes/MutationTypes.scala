package stryker4s.extensions.mutationtypes

import scala.meta.Term.{Apply, Block, Name, Select, Function}
import scala.meta.contrib._
import scala.meta.{Lit, Term, Tree}

/** Base trait for mutations. Mutations can be used to pattern match on (see MutantMatcher). <br>
  * Can also implicitly be converted to the appropriate `scala.meta.Tree` by importing [[stryker4s.extensions.ImplicitMutationConversion]]
  *
  * @tparam T Has to be a subtype of Tree.
  *           This is so that the tree value and unapply methods return the appropriate type.
  *           E.G. A False is of type `scala.meta.Lit.Boolean` instead of a standard `scala.meta.Term`
  */
sealed trait Mutation[T <: Tree] {
  val tree: T
  def unapply(arg: T): Option[T] =
    Some(arg)
      .filter(_.isEqual(tree))
      .filterNot {
        case name: Term.Name => name.isDefinition
        case _ => false
      }
}

trait BinaryOperator extends Mutation[Term.Name]

trait BooleanSubstitution extends Mutation[Lit.Boolean]

trait LogicalOperator extends Mutation[Term.Name]

// TODO: check another mutators (maybe there is a better way to match them), find a common interface
trait MethodMutator {
  protected val methodName: String
  def apply(f: String => Term): Term = f(methodName)
  def unapply(term:Term): Option[(Term, String => Term)]
}

trait OneArgMethodMutator extends MethodMutator {

  def unapply(term: Term): Option[(Term, String => Term)] = term match {
    case Apply(Select(q, Name(`methodName`)), Block(Function(_ :: _ :: _, _) :: Nil) :: Nil) =>
      None
    case Apply(Select(q, Name(`methodName`)), Function(_ :: _ :: _, _) :: Nil) =>
      None
    case Apply(Select(q, Name(`methodName`)), arg :: Nil) =>
      Option(term, name => Apply(Term.Select(q, Name(name)), arg :: Nil))
    case _ => None
  }

}

trait NonArgsMethodMutator extends MethodMutator {

  def unapply(term: Term): Option[(Term, String => Term)] = term match {
    case Select(q, Name(`methodName`)) => Option(term, name => Select(q, Name(name)))
    case _ => None
  }

}

/** T &lt;: Term because it can be either a `Lit.String` or `Term.Interpolation`
  */
trait StringMutator[T <: Term] extends Mutation[T]
