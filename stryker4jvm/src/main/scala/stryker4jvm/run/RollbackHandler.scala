package stryker4jvm.run

import cats.data.NonEmptyList
import stryker4jvm.model.{CompilerErrMsg, MutantResultsPerFile, MutatedFile}

class RollbackHandler {
  def rollbackFiles(
      // errors: NonEmptyList[CompilerErrMsg],
      // allFiles: Seq[MutatedFile]
  ): Either[NonEmptyList[CompilerErrMsg], RollbackResult] = throw new NotImplementedError(
    "Rollback not implemented yet. If you need rollback, use release version v0.14.3"
  )

  // private def compileErrorMutant(mutant: MutantWithId): MutantResult = mutant.toMutantResult(MutantStatus.CompileError)
}

final case class RollbackResult(newFiles: Seq[MutatedFile], compileErrors: MutantResultsPerFile)
