package stryker4s.mutants.findmutants

import stryker4s.extensions.ImplicitMutationConversion.mutationToTree
import stryker4s.extensions.mutationtypes._
import stryker4s.model.Mutant

import scala.meta.Tree

class MutantMatcher extends MutantCreator {

  def allMatchers(): PartialFunction[Tree, Seq[Mutant]] =
    matchBinaryOperators orElse
      matchMethods orElse
      matchString orElse
      matchBooleanSubstitutions

  def matchBinaryOperators(): PartialFunction[Tree, Seq[Mutant]] = {
    case GreaterThanEqualTo(orig) => create(orig, GreaterThan, LesserThan, EqualTo)
    case GreaterThan(orig)        => create(orig, GreaterThanEqualTo, LesserThan, EqualTo)
    case LesserThanEqualTo(orig)  => create(orig, LesserThan, GreaterThanEqualTo, EqualTo)
    case LesserThan(orig)         => create(orig, LesserThanEqualTo, GreaterThan, EqualTo)
    case EqualTo(orig)            => create(orig, NotEqualTo)
    case NotEqualTo(orig)         => create(orig, EqualTo)
    case And(orig)                => create(orig, Or)
    case Or(orig)                 => create(orig, And)
  }

  def matchMethods(): PartialFunction[Tree, Seq[Mutant]] = {
    case Filter(orig)      => create(orig, FilterNot)
    case FilterNot(orig)   => create(orig, Filter)
    case Exists(orig)      => create(orig, ForAll)
    case ForAll(orig)      => create(orig, Exists)
    case IsEmpty(orig)     => create(orig, NonEmpty)
    case NonEmpty(orig)    => create(orig, IsEmpty)
    case IndexOf(orig)     => create(orig, LastIndexOf)
    case LastIndexOf(orig) => create(orig, IndexOf)
    case Max(orig)         => create(orig, Min)
    case Min(orig)         => create(orig, Max)
  }

  def matchString(): PartialFunction[Tree, Seq[Mutant]] = {
    case EmptyString(orig)         => create(orig, StrykerWasHereString)
    case NonEmptyString(orig)      => create(orig, EmptyString)
    case StringInterpolation(orig) => create(orig, EmptyStringInterpolation)
  }

  def matchBooleanSubstitutions(): PartialFunction[Tree, Seq[Mutant]] = {
    case True(orig)  => create(orig, False)
    case False(orig) => create(orig, True)
  }

}
