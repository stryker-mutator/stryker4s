package stryker4s.run

import stryker4s.model._
import stryker4s.mutants.findmutants.SourceCollector

trait MutantRunner {
  def apply(mutatedFiles: Iterable[MutatedFile], fileCollector: SourceCollector): MutantRunResults
}
