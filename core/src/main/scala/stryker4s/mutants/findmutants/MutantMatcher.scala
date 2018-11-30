package stryker4s.mutants.findmutants

import stryker4s.config.Config
import stryker4s.extensions.mutationtypes._
import stryker4s.model.Mutant

import scala.meta.{Term, Tree}

class MutantMatcher()(implicit config: Config) {

  private[this] val stream = Iterator.from(0)

  def allMatchers(): PartialFunction[Tree, Seq[Option[Mutant]]] =
    matchBinaryOperators() orElse
      matchBooleanSubstitutions() orElse
      matchLogicalOperators() orElse
      matchStringMutators() orElse
      matchMethodMutators()

  def matchBinaryOperators(): PartialFunction[Tree, Seq[Option[Mutant]]] = {
    case GreaterThanEqualTo(orig) => orig ~~> (GreaterThan, LesserThan, EqualTo)
    case GreaterThan(orig)        => orig ~~> (GreaterThanEqualTo, LesserThan, EqualTo)
    case LesserThanEqualTo(orig)  => orig ~~> (LesserThan, GreaterThanEqualTo, EqualTo)
    case LesserThan(orig)         => orig ~~> (LesserThanEqualTo, GreaterThan, EqualTo)
    case EqualTo(orig)            => orig ~~> NotEqualTo
    case NotEqualTo(orig)         => orig ~~> EqualTo
  }

  def matchBooleanSubstitutions(): PartialFunction[Tree, Seq[Option[Mutant]]] = {
    case True(orig)  => orig ~~> False
    case False(orig) => orig ~~> True
  }

  def matchLogicalOperators(): PartialFunction[Tree, Seq[Option[Mutant]]] = {
    case And(orig) => orig ~~> Or
    case Or(orig)  => orig ~~> And
  }

  def matchStringMutators(): PartialFunction[Tree, Seq[Option[Mutant]]] = {
    case EmptyString(orig)         => orig ~~> StrykerWasHereString
    case NonEmptyString(orig)      => orig ~~> EmptyString
    case StringInterpolation(orig) => orig ~~> EmptyStringInterpolation
  }

  def matchMethodMutators(): PartialFunction[Tree, Seq[Option[Mutant]]] = {
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
    def ~~>[T <: Term](mutated: SubstitutionMutation[T]*): Seq[Option[Mutant]] = {
      mutated.map(mutation => {
        if (config.excludedMutations.exclusions.contains(mutation.mutationName))
          None
        else
          Some(Mutant(stream.next, original, mutation.tree, mutation))
      })
    }

    def ~~>(mutated: MethodMutator, f: String => Term): Seq[Option[Mutant]] = {
      if (config.excludedMutations.exclusions.contains(mutated.mutationName))
        None :: Nil
      else
        Some(Mutant(stream.next, original, mutated.apply(f), mutated)) :: Nil
    }
  }

}
