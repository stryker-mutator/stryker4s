package stryker4s.model

import stryker4s.extensions.mutationtypes.Mutation

import scala.language.existentials
import scala.meta.{Term, Tree}

case class Mutant(id: Int, original: Term, mutated: Term, mutationType: Mutation[_ <: Tree])
