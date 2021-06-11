package stryker4s.model

import mutationtesting.Location
import stryker4s.extension.mutationtype.Mutation

import scala.meta.{Term, Tree}
import mutationtesting.MutantResult
import mutationtesting.MutantStatus

final case class Mutant(id: MutantId, original: Term, mutated: Term, mutationType: Mutation[? <: Tree])

// TODO: rename globalId to value?
case class MutantId(globalId: Int) extends AnyVal {
  override def toString: String = globalId.toString
}

final case class MutantWithId(id: MutantId, mutatedCode: MutatedCode) {
  def toMutantResult(status: MutantStatus) = MutantResult(
    id = id.toString(),
    mutatorName = mutatedCode.metadata.mutatorName,
    replacement = mutatedCode.metadata.replacement,
    location = mutatedCode.metadata.location,
    status = status
  )
}

/** A piece of mutated code
  */
final case class MutatedCode(mutatedStatement: Tree, metadata: MutantMetadata)

/** Metadata of [[stryker4s.model.MutatedCode]]
  * @param original
  *   Original code
  * @param replacement
  *   Mutated replaced code
  * @param location
  *   The location of the mutated code
  */
final case class MutantMetadata(original: String, replacement: String, mutatorName: String, location: Location)
