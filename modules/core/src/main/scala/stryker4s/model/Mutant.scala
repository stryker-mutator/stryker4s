package stryker4s.model

import cats.syntax.all.*
import mutationtesting.{Location, MutantResult, MutantStatus}
import stryker4s.extension.TreeExtensions.PositionExtension
import stryker4s.testrunner.api.TestDefinitionId

import scala.meta.{Position, Term}

final case class MutantWithId(id: MutantId, mutatedCode: MutatedCode) {
  def toMutantResult(
      status: MutantStatus,
      testsCompleted: Option[Int] = none,
      statusReason: Option[String] = none,
      killedBy: Option[Seq[TestDefinitionId]] = none,
      coveredBy: Option[Seq[TestDefinitionId]] = none
  ): MutantResult =
    MutantResult(
      id = id.toString(),
      mutatorName = mutatedCode.metadata.mutatorName,
      replacement = mutatedCode.metadata.replacement,
      location = mutatedCode.metadata.location,
      status = status,
      description = mutatedCode.metadata.description,
      statusReason = statusReason,
      testsCompleted = testsCompleted,
      coveredBy = coveredBy.map(_.map(_.toString)),
      killedBy = killedBy.map(_.map(_.toString))
    )
}

final case class MutatedCode(mutatedStatement: Term, metadata: MutantMetadata)

/** Metadata of [[stryker4s.model.MutatedCode]]
  *
  * @param original
  *   Original code
  * @param replacement
  *   Mutated replaced code
  * @param mutatorName
  *   Mutator category
  * @param location
  *   The location of the mutated code
  */
final case class MutantMetadata(
    original: String,
    replacement: String,
    mutatorName: String,
    location: Location,
    description: Option[String]
)

object MutantMetadata {
  def apply(
      original: String,
      replacement: String,
      mutatorName: String,
      position: Position,
      description: Option[String]
  ): MutantMetadata =
    MutantMetadata(original, replacement, mutatorName, position.toLocation, description)

}
