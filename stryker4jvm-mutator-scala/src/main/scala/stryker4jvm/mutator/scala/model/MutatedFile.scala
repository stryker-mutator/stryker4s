package stryker4jvm.mutator.scala.model

import stryker4jvm.mutator.scala.mutants.MutantsWithId

import scala.meta.Tree

final case class MutatedFile(mutatedSource: Tree, mutants: MutantsWithId)
