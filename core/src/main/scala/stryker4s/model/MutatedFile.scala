package stryker4s.model

import fs2.io.file.Path

import scala.meta.*

final case class MutatedFile(
    fileOrigin: Path,
    tree: Tree,
    mutants: Seq[Mutant],
    nonCompilingMutants: Seq[Mutant],
    excludedMutants: Int
) {

  def mutatedSource: String = {
    tree.syntax
  }
}
