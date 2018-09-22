package stryker4s.mutants.findmutants

import stryker4s.extensions.ImplicitMutationConversion.mutationToTree
import stryker4s.extensions.mutationtypes._
import stryker4s.model.Mutant

import scala.meta.{Term, Tree}

class MutantMatcher {

  private[this] val stream = Iterator.from(0)

  def allMatchers(): PartialFunction[Tree, Seq[Mutant]] =
    matchBinaryOperators() orElse
      matchBooleanSubstitutions() orElse
      matchLogicalOperators() orElse
      matchStringMutators() orElse
      matchMethodMutators()

  def matchBinaryOperators(): PartialFunction[Tree, Seq[Mutant]] = {
    case GreaterThanEqualTo(orig) => orig ~~> (GreaterThan, LesserThan, EqualTo)
    case GreaterThan(orig)        => orig ~~> (GreaterThanEqualTo, LesserThan, EqualTo)
    case LesserThanEqualTo(orig)  => orig ~~> (LesserThan, GreaterThanEqualTo, EqualTo)
    case LesserThan(orig)         => orig ~~> (LesserThanEqualTo, GreaterThan, EqualTo)
    case EqualTo(orig)            => orig ~~> NotEqualTo
    case NotEqualTo(orig)         => orig ~~> EqualTo
  }

  def matchBooleanSubstitutions(): PartialFunction[Tree, Seq[Mutant]] = {
    case True(orig)  => orig ~~> False
    case False(orig) => orig ~~> True
  }

  def matchLogicalOperators(): PartialFunction[Tree, Seq[Mutant]] = {
    case And(orig) => orig ~~> Or
    case Or(orig)  => orig ~~> And
  }

  def matchStringMutators(): PartialFunction[Tree, Seq[Mutant]] = {
    case EmptyString(orig)         => orig ~~> StrykerWasHereString
    case NonEmptyString(orig)      => orig ~~> EmptyString
    case StringInterpolation(orig) => orig ~~> EmptyStringInterpolation
  }

  def matchMethodMutators(): PartialFunction[Tree, Seq[Mutant]] = {
    case Filter(orig)      => orig ~~> FilterNot
    case FilterNot(orig)   => orig ~~> Filter
    case Exists(orig)      => orig ~~> ForAll
    case ForAll(orig)      => orig ~~> Exists
    case IsEmpty(orig)     => orig ~~> NonEmpty
    case NonEmpty(orig)    => orig ~~> IsEmpty
    case IndexOf(orig)     => orig ~~> LastIndexOf
    case LastIndexOf(orig) => orig ~~> IndexOf
    case Max(orig)         => orig ~~> Min
    case Min(orig)         => orig ~~> Max
  }

  implicit class TermExtensions(original: Term) {
    def ~~>(mutated: Mutation[_ <: Term]*): Seq[Mutant] = {
      mutated.map(mutant => Mutant(stream.next(), original, mutant, mutant.mutationName))
    }
  }
}
