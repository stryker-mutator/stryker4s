package stryker4s.model

import scala.meta.{Term, Tree}

import stryker4s.extension.mutationtype.Mutation

final case class Mutant(id: Int, original: Term, mutated: Term, mutationType: Mutation[_ <: Tree])
