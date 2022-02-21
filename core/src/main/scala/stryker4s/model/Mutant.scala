package stryker4s.model

import mutationtesting.{Location, MutantResult, MutantStatus}
import stryker4s.extension.TreeExtensions.PositionExtension
import stryker4s.extension.mutationtype.Mutation

import scala.meta.{Position, Term, Tree}

// TODO: remove
final case class Mutant(id: MutantId, original: Term, mutated: Term, mutationType: Mutation[? <: Tree])

// TODO: rename globalId to value?
final case class MutantId(globalId: Int) extends AnyVal {
  override def toString: String = globalId.toString
}

final case class MutantWithId(id: MutantId, mutatedCode: MutatedCode) {
  def toMutantResult(status: MutantStatus, testsCompleted: Option[Int] = None, description: Option[String] = None) =
    MutantResult(
      id = id.toString(),
      mutatorName = mutatedCode.metadata.mutatorName,
      replacement = mutatedCode.metadata.replacement,
      location = mutatedCode.metadata.location,
      status = status,
      description = description,
      testsCompleted = testsCompleted
    )
}

/** A piece of mutated code
  */
final case class MutatedCode(mutatedStatement: Term, metadata: MutantMetadata)

/** Metadata of [[stryker4s.model.MutatedCode]]
  * @param original
  *   Original code
  * @param replacement
  *   Mutated replaced code
  * @param location
  *   The location of the mutated code
  */
final case class MutantMetadata(original: String, replacement: String, mutatorName: String, location: Location)

object MutantMetadata {
  def apply(original: String, replacement: String, mutatorName: String, position: Position): MutantMetadata =
    MutantMetadata(original, replacement, mutatorName, position.toLocation)
}
