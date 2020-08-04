package stryker4s.mutants.findmutants

import scala.meta._

import stryker4s.config.Config
import stryker4s.extension.TreeExtensions.{GetMods, PathToRoot, TreeIsInExtension}
import stryker4s.extension.mutationtype._
import stryker4s.model.Mutant

class MutantMatcher()(implicit config: Config) {
  private[this] val ids = Iterator.from(0)

  def allMatchers: PartialFunction[Tree, Seq[Option[Mutant]]] =
    matchBooleanLiteral orElse
      matchEqualityOperator orElse
      matchLogicalOperator orElse
      matchConditionalExpression orElse
      matchMethodExpression orElse
      matchStringLiteral

  def matchBooleanLiteral: PartialFunction[Tree, Seq[Option[Mutant]]] = {
    case True(orig)  => orig ~~> False
    case False(orig) => orig ~~> True
  }

  def matchEqualityOperator: PartialFunction[Tree, Seq[Option[Mutant]]] = {
    case GreaterThanEqualTo(orig) => orig.~~>(GreaterThan, LesserThan, EqualTo)
    case GreaterThan(orig)        => orig.~~>(GreaterThanEqualTo, LesserThan, EqualTo)
    case LesserThanEqualTo(orig)  => orig.~~>(LesserThan, GreaterThanEqualTo, EqualTo)
    case LesserThan(orig)         => orig.~~>(LesserThanEqualTo, GreaterThan, EqualTo)
    case EqualTo(orig)            => orig ~~> NotEqualTo
    case NotEqualTo(orig)         => orig ~~> EqualTo
    case TypedEqualTo(orig)       => orig ~~> TypedNotEqualTo
    case TypedNotEqualTo(orig)    => orig ~~> TypedEqualTo
  }

  def matchLogicalOperator: PartialFunction[Tree, Seq[Option[Mutant]]] = {
    case And(orig) => orig ~~> Or
    case Or(orig)  => orig ~~> And
  }

  def matchConditionalExpression: PartialFunction[Tree, Seq[Option[Mutant]]] = {
    case If(condition)      => condition.~~>(ConditionalTrue, ConditionalFalse)
    case While(condition)   => condition ~~> ConditionalFalse
    case DoWhile(condition) => condition ~~> ConditionalFalse
  }

  def matchMethodExpression: PartialFunction[Tree, Seq[Option[Mutant]]] = {
    case Filter(orig, f)      => orig.~~>(f, FilterNot)
    case FilterNot(orig, f)   => orig.~~>(f, Filter)
    case Exists(orig, f)      => orig.~~>(f, Forall)
    case Forall(orig, f)      => orig.~~>(f, Exists)
    case Take(orig, f)        => orig.~~>(f, Drop)
    case Drop(orig, f)        => orig.~~>(f, Take)
    case TakeRight(orig, f)   => orig.~~>(f, DropRight)
    case DropRight(orig, f)   => orig.~~>(f, TakeRight)
    case TakeWhile(orig, f)   => orig.~~>(f, DropWhile)
    case DropWhile(orig, f)   => orig.~~>(f, TakeWhile)
    case IsEmpty(orig, f)     => orig.~~>(f, NonEmpty)
    case NonEmpty(orig, f)    => orig.~~>(f, IsEmpty)
    case IndexOf(orig, f)     => orig.~~>(f, LastIndexOf)
    case LastIndexOf(orig, f) => orig.~~>(f, IndexOf)
    case Max(orig, f)         => orig.~~>(f, Min)
    case Min(orig, f)         => orig.~~>(f, Max)
    case MaxBy(orig, f)       => orig.~~>(f, MinBy)
    case MinBy(orig, f)       => orig.~~>(f, MaxBy)
  }

  def matchStringLiteral: PartialFunction[Tree, Seq[Option[Mutant]]] = {
    case EmptyString(orig)         => orig ~~> StrykerWasHereString
    case NonEmptyString(orig)      => orig ~~> EmptyString
    case StringInterpolation(orig) => orig ~~> EmptyStringInterpolation
  }

  implicit class TermExtensions(original: Term) {
    def ~~>[T <: Term](mutated: SubstitutionMutation[T]*): Seq[Option[Mutant]] =
      createMutants[SubstitutionMutation[T]](mutated, _.tree)

    def ~~>(f: String => Term, mutated: MethodExpression*): Seq[Option[Mutant]] =
      createMutants[MethodExpression](mutated, _(f))

    private def createMutants[T <: Mutation[_ <: Tree]](
        mutations: Seq[T],
        mutationToTerm: T => Term
    ): Seq[Option[Mutant]] =
      ifNotInAnnotation {
        mutations
          .map { mutated =>
            if (matchExcluded(mutated) || isSuppressedByAnnotation(mutated, original))
              None
            else
              Some(Mutant(ids.next(), original, mutationToTerm(mutated), mutated))
          }
      }

    private def ifNotInAnnotation(maybeMutants: => Seq[Option[Mutant]]): Seq[Option[Mutant]] = {
      if (original.isIn[Mod.Annot])
        Nil
      else
        maybeMutants
    }

    private def matchExcluded(mutation: Mutation[_]): Boolean = {
      config.excludedMutations.contains(mutation.mutationName)
    }

    private def isSuppressedByAnnotation(mutation: Mutation[_], original: Term): Boolean = {
      original.pathToRoot.flatMap(_.getMods).exists(isSupressWarningsAnnotation(_, mutation))
    }

    private def isSupressWarningsAnnotation(mod: Mod, mutation: Mutation[_]) = {
      val mutationName = "stryker4s.mutation." + mutation.mutationName
      mod match {
        case Mod.Annot(Init(Type.Name("SuppressWarnings"), _, List(List(Term.Apply(Name("Array"), params))))) =>
          params.exists {
            case Lit.String(`mutationName`) => true
            case _                          => false
          }
        case _ => false
      }
    }

  }
}
