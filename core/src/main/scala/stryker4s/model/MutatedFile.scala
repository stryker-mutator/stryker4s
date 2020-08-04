package stryker4s.model

import scala.meta.Tree

import better.files.File

final case class MutatedFile(fileOrigin: File, tree: Tree, mutants: Seq[Mutant], excludedMutants: Int)
