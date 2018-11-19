package stryker4s.mutants

import stryker4s.extensions.mutationtypes._
import stryker4s.model.Mutant

case class Exclusions(exclusions: Set[String]) {

  def shouldExclude(mutant: Mutant): Boolean = {
    mutant.mutationType match {
      case _: BinaryOperator if exclusions("BinaryOperator")           => true
      case _: BooleanSubstitution if exclusions("BooleanSubstitution") => true
      case _: LogicalOperator if exclusions("LogicalOperator")         => true
      case _: StringMutator[_] if exclusions("StringMutator")          => true
      case _: MethodMutator if exclusions("MethodMutator")             => true
      case _                                                           => false
    }
  }
}
