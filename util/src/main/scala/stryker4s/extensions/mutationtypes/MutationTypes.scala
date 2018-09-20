package stryker4s.extensions.mutationtypes

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

  def unapply(arg: T): Option[T] = Some(arg).filter(a => a.isEqual(tree))
}

trait BinaryOperator extends Mutation[Term.Name]

trait BooleanSubstitution extends Mutation[Lit.Boolean]

trait LogicalOperator extends Mutation[Term.Name]

trait MethodMutator extends Mutation[Term.Name]

/** T &lt;: Term because it can be either a `Lit.String` or `Term.Interpolation`
  */
trait StringMutator[T <: Term] extends Mutation[T]
