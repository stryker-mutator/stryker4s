package stryker4s.model

import scala.meta.Source

final case class MutationsInSource(source: Source, mutants: Seq[Mutant], excluded: Int)
