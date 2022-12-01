package stryker4jvm.run

import cats.data.NonEmptyList

class RollbackHandler {
  def rollbackFiles(
      errors: NonEmptyList[CompilerErrMsg],
      allFiles: Seq[MutatedFile]
  ): Either[NonEmptyList[CompilerErrMsg], RollbackResult] = ???

  // private def compileErrorMutant(mutant: MutantWithId): MutantResult = mutant.toMutantResult(MutantStatus.CompileError)
}

final case class RollbackResult(newFiles: Seq[MutatedFile], compileErrors: MutantResultsPerFile)
