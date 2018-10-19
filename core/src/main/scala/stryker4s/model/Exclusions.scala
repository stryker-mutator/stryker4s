package stryker4s.model

import stryker4s.extensions.mutationtypes._
case class Exclusions(val exclusionStrings: String*) {
  private lazy val set = Set(exclusionStrings: _*)

  def shouldExclude(mutant: Mutant): Boolean = mutant match {
    case _: BinaryOperator if set("BinaryOperator")           => true
    case _: BooleanSubstitution if set("BooleanSubstitution") => true
    case _: LogicalOperator if set("LogicalOperator")         => true
    case _: StringMutator[_] if set("StringMutator")          => true
    case _: MethodMutator if set("MethodMutator")             => true
    case _                                                    => false
  }
}
