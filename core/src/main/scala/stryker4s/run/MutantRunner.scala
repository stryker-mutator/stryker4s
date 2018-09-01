package stryker4s.run

import stryker4s.model._

trait MutantRunner {
  def apply(files: Iterable[MutatedFile]): MutantRunResults
}
