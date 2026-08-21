package stryker4s.mutatorapi

import mutationtesting.{Location, Position as MTPosition}

import scala.meta.Position

/** Metadata of a [[stryker4s.mutatorapi.MutatedCode]]
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

  /** Convenience constructor that converts a scalameta `Position` to the `Location` used in reports.
    */
  def apply(
      original: String,
      replacement: String,
      mutatorName: String,
      position: Position,
      description: Option[String]
  ): MutantMetadata =
    MutantMetadata(original, replacement, mutatorName, toLocation(position), description)

  private def toLocation(pos: Position): Location = Location(
    start = MTPosition(line = pos.startLine + 1, column = pos.startColumn + 1),
    end = MTPosition(line = pos.endLine + 1, column = pos.endColumn + 1)
  )
}
