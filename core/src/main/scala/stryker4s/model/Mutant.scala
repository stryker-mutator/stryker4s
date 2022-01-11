package stryker4s.model

import scala.meta.{Term, Tree}

import stryker4s.extension.mutationtype.Mutation

case class MutantId(globalId: Int) extends AnyVal {
  override def toString: String = globalId.toString
}

final case class Mutant(id: MutantId, original: Term, mutated: Term, mutationType: Mutation[? <: Tree])
