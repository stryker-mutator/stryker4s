package stryker4s.model

import scala.meta.{Term, Tree}

import stryker4s.extension.mutationtype.Mutation

//The globalId is used in mutation switching and generating reports, but it varies between runs
//The file and idInFile is stable, and used for finding compiler errors
case class MutantId(globalId: Int, file: String, idInFile: Int) {
  def sameMutation(otherMutId: MutantId): Boolean = {
    otherMutId.idInFile == this.idInFile && otherMutId.file == this.file
  }
}

object MutantId {
  //Initially mutants are created with just a globalId, the file information is added later on
  def apply(globalId: Int): MutantId = new MutantId(globalId, "", -1)
}

final case class Mutant(id: MutantId, original: Term, mutated: Term, mutationType: Mutation[_ <: Tree])
