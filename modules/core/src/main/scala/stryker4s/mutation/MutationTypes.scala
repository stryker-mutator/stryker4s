package stryker4s.mutation

import cats.syntax.option.*
import stryker4s.extension.TreeExtensions.IsEqualExtension

import scala.meta.*
import scala.meta.internal.trees.XtensionTreesName

/** Base trait for mutations. Mutations can be used to pattern match on (see MutantMatcher).
  */
sealed trait Mutation[+T <: Tree] {
  def mutationName: String

  /** Full name including the package.
    *
    * E.g. stryker4s.mutation.BooleanLiteral
    */
  def fullName: String
}

object Mutation {
  // List of mutations
  val mutations: List[String] = List[String](
    classOf[EqualityOperator].getSimpleName,
    classOf[BooleanLiteral].getSimpleName,
    classOf[ConditionalExpression].getSimpleName,
    classOf[LogicalOperator].getSimpleName,
    classOf[StringLiteral[?]].getSimpleName,
    classOf[MethodExpression].getSimpleName,
    classOf[RegularExpression].getSimpleName()
  )
}

/** Base trait for substitution mutation
  *
  * @tparam T
  *   Has to be a subtype of Tree. This is so that the tree value and unapply methods return the appropriate type. E.G.
  *   a False is of type `scala.meta.Lit.Boolean` instead of a standard `scala.meta.Term`
  */
trait SubstitutionMutation[T <: Tree] extends Mutation[T] with NoInvalidPlacement[T] {
  def tree: T

  override def unapply(arg: T): Option[T] =
    arg.some
      .filter(_.isEqual(tree))
      .flatMap(super.unapply)
}

trait EqualityOperator extends SubstitutionMutation[Term.Name] {
  override val mutationName: String = classOf[EqualityOperator].getSimpleName
  override def fullName: String = classOf[EqualityOperator].getName
}

trait BooleanLiteral extends SubstitutionMutation[Lit.Boolean] {
  override val mutationName: String = classOf[BooleanLiteral].getSimpleName
  override def fullName: String = classOf[BooleanLiteral].getName
}

trait ConditionalExpression extends SubstitutionMutation[Lit.Boolean] {
  override val mutationName: String = classOf[ConditionalExpression].getSimpleName
  override def fullName: String = classOf[ConditionalExpression].getName
}

trait LogicalOperator extends SubstitutionMutation[Term.Name] {
  override val mutationName: String = classOf[LogicalOperator].getSimpleName
  override def fullName: String = classOf[LogicalOperator].getName
}

/** T &lt;: Term because it can be either a `Lit.String` or `Term.Interpolation`
  */
trait StringLiteral[T <: Term] extends SubstitutionMutation[T] {
  override val mutationName: String = classOf[StringLiteral[?]].getSimpleName
  override def fullName: String = classOf[StringLiteral[?]].getName
}

/** Base trait for method mutation
  */
trait MethodExpression extends Mutation[Term] {

  /** Method to be replaced or to replace
    */
  protected val methodName: String

  override val mutationName: String = classOf[MethodExpression].getSimpleName
  override def fullName: String = classOf[MethodExpression].getName

  def apply(f: String => Term): Term = f(methodName)

  def unapply(term: Term): Option[(Term, String => Term)]
}

/** Helper extractor to filter out mutants that syntactically can not be placed
  */
protected trait NoInvalidPlacement[T <: Tree] {
  def unapply(arg: T): Option[T] =
    arg.some
      .filterNot {
        case name: Term.Name       => name.isDefinition
        case ParentIsTypeLiteral() => true
        case _                     => false
      }
}

private[stryker4s] case object ParentIsTypeLiteral {
  def unapply(t: Tree): Boolean = t.parent.exists {
    case Type.ArgClause(expprs) if expprs.contains(t) => true
    case Defn.Val(_, _, Some(`t`), _)                 => true
    case Defn.Var.Initial(_, _, Some(`t`), _)         => true
    case Defn.Def.Initial(_, _, _, _, Some(`t`), _)   => true
    case Defn.Type.Initial(_, _, _, `t`)              => true
    case p                                            => p.is[Type] || p.is[Term.ApplyType]
  }
}
