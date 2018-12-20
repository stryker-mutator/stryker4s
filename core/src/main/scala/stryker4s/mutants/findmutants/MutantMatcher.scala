package stryker4s.mutants.findmutants

import stryker4s.extensions.TreeExtensions.ImplicitTreeExtensions
import stryker4s.config.Config
import stryker4s.extensions.mutationtypes._
import stryker4s.model.Mutant

import scala.meta.{Term, Tree}

class MutantMatcher()(implicit config: Config) {

  private[this] val stream = Iterator.from(0)

  def allMatchers(): PartialFunction[Tree, Seq[Option[Mutant]]] =
    matchEqualityOperator() orElse
      matchBooleanLiteral() orElse
      matchLogicalOperator() orElse
      matchStringLiteral() orElse
      matchMethodExpression()

  def matchEqualityOperator(): PartialFunction[Tree, Seq[Option[Mutant]]] = {
    case GreaterThanEqualTo(orig) => orig ~~> (GreaterThan, LesserThan, EqualTo)
    case GreaterThan(orig)        => orig ~~> (GreaterThanEqualTo, LesserThan, EqualTo)
    case LesserThanEqualTo(orig)  => orig ~~> (LesserThan, GreaterThanEqualTo, EqualTo)
    case LesserThan(orig)         => orig ~~> (LesserThanEqualTo, GreaterThan, EqualTo)
    case EqualTo(orig)            => orig ~~> NotEqualTo
    case NotEqualTo(orig)         => orig ~~> EqualTo
  }

  def matchBooleanLiteral(): PartialFunction[Tree, Seq[Option[Mutant]]] = {
    case True(orig)  => orig ~~> False
    case False(orig) => orig ~~> True
  }

  def matchLogicalOperator(): PartialFunction[Tree, Seq[Option[Mutant]]] = {
    case And(orig) => orig ~~> Or
    case Or(orig)  => orig ~~> And
  }

  def matchStringLiteral(): PartialFunction[Tree, Seq[Option[Mutant]]] = {
    case EmptyString(orig)         => orig ~~> StrykerWasHereString
    case NonEmptyString(orig)      => orig ~~> EmptyString
    case StringInterpolation(orig) => orig ~~> EmptyStringInterpolation
  }

  def matchMethodExpression(): PartialFunction[Tree, Seq[Option[Mutant]]] = {
    case Filter(orig, f)      => orig ~~> (FilterNot, f)
    case FilterNot(orig, f)   => orig ~~> (Filter, f)
    case Exists(orig, f)      => orig ~~> (ForAll, f)
    case ForAll(orig, f)      => orig ~~> (Exists, f)
    case Take(orig, f)        => orig ~~> (Drop, f)
    case Drop(orig, f)        => orig ~~> (Take, f)
    case IsEmpty(orig, f)     => orig ~~> (NonEmpty, f)
    case NonEmpty(orig, f)    => orig ~~> (IsEmpty, f)
    case IndexOf(orig, f)     => orig ~~> (LastIndexOf, f)
    case LastIndexOf(orig, f) => orig ~~> (IndexOf, f)
    case Max(orig, f)         => orig ~~> (Min, f)
    case Min(orig, f)         => orig ~~> (Max, f)
    case MaxBy(orig, f)       => orig ~~> (MinBy, f)
    case MinBy(orig, f)       => orig ~~> (MaxBy, f)
  }

  implicit class TermExtensions(original: Term) {
    def ~~>[T <: Term](mutated: SubstitutionMutation[T]*): Seq[Option[Mutant]] = ifNotInAnnotation {
      mutated.map(mutation => {
        if (matchExcluded(mutation))
          None
        else
          Some(Mutant(stream.next, original, mutation.tree, mutation))
      })
    }
    def ~~>(mutated: MethodExpression, f: String => Term): Seq[Option[Mutant]] = ifNotInAnnotation {
      if (matchExcluded(mutated))
        None :: Nil
      else
        Some(Mutant(stream.next, original, mutated.apply(f), mutated)) :: Nil
    }

    private def ifNotInAnnotation(fun: => Seq[Option[Mutant]]): Seq[Option[Mutant]] = {
      if (original.isInAnnotation) Nil
      else fun
    }

    private def matchExcluded(mutation: Mutation[_]): Boolean = {
      config.excludedMutations.contains(mutation.mutationName)
    }
  }

}
