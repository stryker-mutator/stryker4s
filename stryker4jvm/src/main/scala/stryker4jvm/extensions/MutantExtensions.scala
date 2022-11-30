package stryker4jvm.extensions

import mutationtesting.{MutantResult, MutantStatus}
import stryker4jvm.core.model.{AST, MutantWithId}

object MutantExtensions {
  implicit final class ToMutantResultExtension(val mutantWithId: MutantWithId[AST]) {
    final def toMutantResult(
        status: MutantStatus,
        testsCompleted: Option[Int] = None,
        description: Option[String] = None
    ) = {
      MutantResult(
        id = mutantWithId.id.toString(),
        mutatorName = mutantWithId.mutatedCode.metaData.mutatorName,
        replacement = mutantWithId.mutatedCode.metaData.replacement,
        location = mutantWithId.mutatedCode.metaData.location,
        status = status,
        description = description,
        testsCompleted = testsCompleted
      )
    }
  }
}
