package stryker4s.run

import cats.data.NonEmptyList
import stryker4s.model.{CompilerErrMsg, MutantResultsPerFile, MutatedFile}

trait RollbackHandler {
  def rollbackFiles(
      errors: NonEmptyList[CompilerErrMsg],
      allFiles: Seq[MutatedFile]
  ): Either[NonEmptyList[CompilerErrMsg], RollbackResult]
}

final case class RollbackResult(newFiles: Seq[MutatedFile], compileErrors: MutantResultsPerFile)
