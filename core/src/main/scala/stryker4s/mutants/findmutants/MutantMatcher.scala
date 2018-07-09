package stryker4s.mutants.findmutants

import stryker4s.extensions.ImplicitMutationConversion.mutationToTree
import stryker4s.extensions.mutationtypes._
import stryker4s.model.FoundMutant

import scala.meta.Tree

class MutantMatcher {

  def allMatchers(): PartialFunction[Tree, FoundMutant] =
    matchConditionals() orElse
      matchMethods() orElse
      matchLiterals()

  def matchConditionals(): PartialFunction[Tree, FoundMutant] = {
    case GreaterThanEqualTo(orig) => FoundMutant(orig, GreaterThan, LesserThan, EqualTo)
    case GreaterThan(orig)        => FoundMutant(orig, GreaterThanEqualTo, LesserThan, EqualTo)
    case LesserThanEqualTo(orig)  => FoundMutant(orig, LesserThan, GreaterThanEqualTo, EqualTo)
    case LesserThan(orig)         => FoundMutant(orig, LesserThanEqualTo, GreaterThan, EqualTo)
    case EqualTo(orig)            => FoundMutant(orig, NotEqualTo)
    case NotEqualTo(orig)         => FoundMutant(orig, EqualTo)
    case And(orig)                => FoundMutant(orig, Or)
    case Or(orig)                 => FoundMutant(orig, And)
  }

  def matchMethods(): PartialFunction[Tree, FoundMutant] = {
    case Filter(orig)    => FoundMutant(orig, FilterNot)
    case FilterNot(orig) => FoundMutant(orig, Filter)
  }

  def matchLiterals(): PartialFunction[Tree, FoundMutant] = {
    case True(orig)  => FoundMutant(orig, False)
    case False(orig) => FoundMutant(orig, True)
  }

}
