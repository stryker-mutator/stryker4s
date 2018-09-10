package stryker4s.mutants.findmutants

import stryker4s.extensions.ImplicitMutationConversion.mutationToTree
import stryker4s.extensions.mutationtypes._
import stryker4s.model.{Mutant, MutantCreator}

import scala.meta.Tree

class MutantMatcher {

  def allMatchers(): PartialFunction[Tree, Seq[Mutant]] =
    matchBinaryOperators orElse
      matchMethods orElse
      matchString orElse
      matchBooleanSubstitutions

  def matchBinaryOperators(): PartialFunction[Tree, Seq[Mutant]] = {
    case GreaterThanEqualTo(orig) => MutantCreator.create(orig, GreaterThan, LesserThan, EqualTo)
    case GreaterThan(orig)        => MutantCreator.create(orig, GreaterThanEqualTo, LesserThan, EqualTo)
    case LesserThanEqualTo(orig)  => MutantCreator.create(orig, LesserThan, GreaterThanEqualTo, EqualTo)
    case LesserThan(orig)         => MutantCreator.create(orig, LesserThanEqualTo, GreaterThan, EqualTo)
    case EqualTo(orig)            => MutantCreator.create(orig, NotEqualTo)
    case NotEqualTo(orig)         => MutantCreator.create(orig, EqualTo)
    case And(orig)                => MutantCreator.create(orig, Or)
    case Or(orig)                 => MutantCreator.create(orig, And)
  }

  def matchMethods(): PartialFunction[Tree, Seq[Mutant]] = {
    case Filter(orig)      => MutantCreator.create(orig, FilterNot)
    case FilterNot(orig)   => MutantCreator.create(orig, Filter)
    case Exists(orig)      => MutantCreator.create(orig, ForAll)
    case ForAll(orig)      => MutantCreator.create(orig, Exists)
    case IsEmpty(orig)     => MutantCreator.create(orig, NonEmpty)
    case NonEmpty(orig)    => MutantCreator.create(orig, IsEmpty)
    case IndexOf(orig)     => MutantCreator.create(orig, LastIndexOf)
    case LastIndexOf(orig) => MutantCreator.create(orig, IndexOf)
    case Max(orig)         => MutantCreator.create(orig, Min)
    case Min(orig)         => MutantCreator.create(orig, Max)
  }

  def matchString(): PartialFunction[Tree, Seq[Mutant]] = {
    case EmptyString(orig)         => MutantCreator.create(orig, StrykerWasHereString)
    case NonEmptyString(orig)      => MutantCreator.create(orig, EmptyString)
    case StringInterpolation(orig) => MutantCreator.create(orig, EmptyStringInterpolation)
  }

  def matchBooleanSubstitutions(): PartialFunction[Tree, Seq[Mutant]] = {
    case True(orig)  => MutantCreator.create(orig, False)
    case False(orig) => MutantCreator.create(orig, True)
  }

}
