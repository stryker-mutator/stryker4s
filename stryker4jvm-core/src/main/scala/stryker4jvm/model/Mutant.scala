package stryker4jvm.model

import mutationtesting.{Location, MutantResult, MutantStatus}
import stryker4jvm.core.model.MutatedCode

final case class MutantId(value: Int) extends AnyVal {
  override def toString(): String = value.toString
}

final case class MutantWithId[T](id: MutantId, mutatedCode: MutatedCode[T]) {
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

final case class MutatedCode[T](mutatedStatement: T, metadata: MutantMetadata)

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
final case class MutantMetadata(original: String, replacement: String, mutatorName: String, location: Location) {
  def showLocation: String = {
    s"${location.start.line}:${location.start.column} to ${location.end.line}:${location.end.column}"
  }
}
