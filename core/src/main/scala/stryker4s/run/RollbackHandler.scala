package stryker4s.run

import cats.data.NonEmptyList
import stryker4s.model.{CompilerErrMsg, MutantResultsPerFile, MutatedFile}

class RollbackHandler {
  def rollbackFiles(
      errors: NonEmptyList[CompilerErrMsg],
      allFiles: Seq[MutatedFile]
  ): Either[NonEmptyList[CompilerErrMsg], RollbackResult] = ???

  // private def compileErrorMutant(mutant: MutantWithId): MutantResult = mutant.toMutantResult(MutantStatus.CompileError)
}

final case class RollbackResult(newFiles: Seq[MutatedFile], compileErrors: MutantResultsPerFile)
