package stryker4s.mutants

import stryker4s.extensions.mutationtypes._
import stryker4s.model.Mutant

case class Exclusions(exclusionStrings: String*) {

  private lazy val set: Set[String] = Set(exclusionStrings: _*)

  def shouldExclude(mutant: Mutant): Boolean = {

    mutant.mutationType match {
      case _: BinaryOperator if set("BinaryOperator")           => true
      case _: BooleanSubstitution if set("BooleanSubstitution") => true
      case _: LogicalOperator if set("LogicalOperator")         => true
      case _: StringMutator[_] if set("StringMutator")          => true
      case _: MethodMutator if set("MethodMutator")             => true
      case _                                                    => false
    }
  }
}
