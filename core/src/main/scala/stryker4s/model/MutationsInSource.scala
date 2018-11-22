package stryker4s.model

import scala.meta.Source

case class MutationsInSource(source: Source, mutants: Seq[Mutant], excluded: Seq[Mutant])
