package stryker4s.mutants

import stryker4s.model.Mutant

case class Exclusions(exclusions: Set[String]) {

  def shouldExclude(mutant: Mutant): Boolean = {
    exclusions.contains(mutant.mutationType.mutationName)
  }
}
