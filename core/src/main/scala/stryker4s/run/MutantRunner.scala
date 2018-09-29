package stryker4s.run

import better.files.File
import stryker4s.model._

trait MutantRunner {
  def apply(files: Iterable[File], mutatedFiles: Iterable[MutatedFile]): MutantRunResults
}
