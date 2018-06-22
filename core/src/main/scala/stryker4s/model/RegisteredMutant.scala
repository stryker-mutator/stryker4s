package stryker4s.model

import scala.meta.{Source, Term}

case class RegisteredMutant(originalStatement: Term, mutants: Seq[Mutant])

case class MutationsInSource(source: Source, mutants: Seq[RegisteredMutant])
