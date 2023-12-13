package stryker4s.model

import cats.Show
import cats.syntax.all.*
import mutationtesting.{Location, MutantResult, MutantStatus}
import stryker4s.extension.TreeExtensions.PositionExtension

import scala.meta.{Position, Term}

final case class MutantId(value: Int) extends AnyVal {
  override def toString(): String = value.toString
}

final case class MutantWithId(id: MutantId, mutatedCode: MutatedCode) {
  def toMutantResult(
      status: MutantStatus,
      testsCompleted: Option[Int] = none,
      statusReason: Option[String] = none
  ): MutantResult =
    MutantResult(
      id = id.toString(),
      mutatorName = mutatedCode.metadata.mutatorName,
      replacement = mutatedCode.metadata.replacement,
      location = mutatedCode.metadata.location,
      status = status,
      description = mutatedCode.metadata.description,
      statusReason = statusReason,
      testsCompleted = testsCompleted
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
) {
  def showLocation: String = {
    import MutantMetadata.locationShow

    location.show
  }
}

object MutantMetadata {
  def apply(
      original: String,
      replacement: String,
      mutatorName: String,
      position: Position,
      description: Option[String]
  ): MutantMetadata =
    MutantMetadata(original, replacement, mutatorName, position.toLocation, description)

  implicit def locationShow: Show[Location] = Show.show(location =>
    s"${location.start.line}:${location.start.column} to ${location.end.line}:${location.end.column}"
  )
}
