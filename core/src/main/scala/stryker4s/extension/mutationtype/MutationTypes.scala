package stryker4s.extension.mutationtype

import scala.meta.{Lit, Term, Tree}

import stryker4s.extension.TreeExtensions.IsEqualExtension

/** Base trait for mutations. Mutations can be used to pattern match on (see MutantMatcher).
  */
sealed trait Mutation[T <: Tree] {
  def mutationName: String
}

object Mutation {
  // List of mutations
  val mutations: List[String] = List[String](
    classOf[EqualityOperator].getSimpleName,
    classOf[BooleanLiteral].getSimpleName,
    classOf[ConditionalExpression].getSimpleName,
    classOf[LogicalOperator].getSimpleName,
    classOf[StringLiteral[_]].getSimpleName,
    classOf[MethodExpression].getSimpleName,
    classOf[RegularExpression].getSimpleName()
  )
}

/** Base trait for substitution mutation
  *
  * Can implicitly be converted to the appropriate `scala.meta.Tree` by importing [[stryker4s.extension.ImplicitMutationConversion]]
  *
  * @tparam T Has to be a subtype of Tree.
  *           This is so that the tree value and unapply methods return the appropriate type.
  *           E.G. A False is of type `scala.meta.Lit.Boolean` instead of a standard `scala.meta.Term`
  */
trait SubstitutionMutation[T <: Tree] extends Mutation[T] {
  def tree: T

  def unapply(arg: T): Option[T] =
    Some(arg)
      .filter(_.isEqual(tree))
      .filterNot {
        case name: Term.Name => name.isDefinition
        case _               => false
      }
}

trait EqualityOperator extends SubstitutionMutation[Term.Name] {
  override val mutationName: String = classOf[EqualityOperator].getSimpleName
}

trait BooleanLiteral extends SubstitutionMutation[Lit.Boolean] {
  override val mutationName: String = classOf[BooleanLiteral].getSimpleName
}

trait ConditionalExpression extends SubstitutionMutation[Lit.Boolean] {
  override val mutationName: String = classOf[ConditionalExpression].getSimpleName
}

trait LogicalOperator extends SubstitutionMutation[Term.Name] {
  override val mutationName: String = classOf[LogicalOperator].getSimpleName
}

/** T &lt;: Term because it can be either a `Lit.String` or `Term.Interpolation`
  */
trait StringLiteral[T <: Term] extends SubstitutionMutation[T] {
  override val mutationName: String = classOf[StringLiteral[_]].getSimpleName
}

/** Base trait for method mutation
  */
trait MethodExpression extends Mutation[Term] {

  /** Method to be replaced or to replace
    */
  protected val methodName: String

  override val mutationName: String = classOf[MethodExpression].getSimpleName

  def apply(f: String => Term): Term = f(methodName)

  def unapply(term: Term): Option[(Term, String => Term)]
}
