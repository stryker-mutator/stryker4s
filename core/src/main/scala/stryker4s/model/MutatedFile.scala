package stryker4s.model

import fs2.io.file.Path

import scala.meta.Tree

final case class MutatedFile(fileOrigin: Path, tree: Tree, mutants: Seq[Mutant], excludedMutants: Int)
