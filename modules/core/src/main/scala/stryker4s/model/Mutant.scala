package stryker4s.model

import cats.syntax.all.*
import mutationtesting.{MutantResult, MutantStatus}
import stryker4s.testrunner.api.TestDefinitionId

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
