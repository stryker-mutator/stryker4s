package stryker4s.mutants

import grizzled.slf4j.Logging
import stryker4s.model.Mutant

case class Exclusions(exclusions: Set[String]) extends Logging{

  def shouldExclude(mutant: Mutant): Boolean = {
    exclusions.contains(mutant.mutationType.mutationName)
  }
}
