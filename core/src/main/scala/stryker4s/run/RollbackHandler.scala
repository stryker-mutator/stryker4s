package stryker4s.run

import cats.data.NonEmptyList
import stryker4s.model.{CompilerErrMsg, MutantResultsPerFile, MutatedFile}
import stryker4s.mutants.tree.MutantsWithId
import stryker4s.model.MutantId
import stryker4s.mutants.tree.MutantInstrumenter
import scala.meta.*
import stryker4s.log.Logger
import cats.syntax.all.*
import stryker4s.model.MutantWithId
import stryker4s.extension.TreeExtensions.*
import cats.data.NonEmptyVector
import mutationtesting.MutantStatus
import cats.data.Ior.Both
import cats.data.Ior
import cats.data.Ior

class RollbackHandler()(implicit log: Logger) {
  def instrumenter: MutantInstrumenter = ???

  def rollbackFiles(
      errors: NonEmptyList[CompilerErrMsg],
      allFiles: Vector[MutatedFile]
  ): Either[NonEmptyList[CompilerErrMsg], RollbackResult] = {
    val fileErrorMap = allFiles.flatMap { mutatedFile =>
      val errorsForFile = NonEmptyList.fromList(
        errors.filter(err => mutatedFile.fileOrigin.toString.endsWith(err.path))
      )
      errorsForFile.tupleLeft(mutatedFile)
    }
    // Go through filesWithErrors, parse the mutatedFile again and remove mutant case statement that have a compile error inside
    val filesWithRemovedErrors = fileErrorMap.toVector
      .traverse { case (mutatedFile, errors) =>
        val parsed = mutatedFile.mutatedSource.syntax
          .parse[Source]
          .get // Should always pass as we already parsed it once

        val treeWithoutErrors = parsed.transform(instrumenter.attemptRemoveMutant(errors))
        val compileErrorMutantIds = errorsToIds(errors, parsed, mutatedFile.mutants)

        // Split into mutants that have a compile error and mutants that don't
        mutatedFile.mutants
          .nonEmptyPartition { mutant =>
            compileErrorMutantIds
              .get(mutant.id)
              .map(error => mutant.toMutantResult(MutantStatus.CompileError, description = error.toString.some))
              .toRight(mutant)
          }
          .map(mutatedFile.fileOrigin -> _.toList.toVector) match {

          // Some mutants were removed, some still remain
          case Both(mutants, results) =>
            (mutatedFile.copy(mutatedSource = treeWithoutErrors, mutants = mutants.toNev).some, results).asRight
          // All mutants were removed
          case Ior.Right(results) => (None, results).asRight
          // No mutants were removed. Something probably went wrong
          case Ior.Left(mutants) => mutants.asLeft
        }
      }
      .map { files =>
        val f = files.map(_._2).toMap
        RollbackResult(files.flatMap(_._1), f)
      }

    throw new NotImplementedError(
      "Rollback not implemented yet. If you need rollback, use release version v0.14.3"
    )
  }

  private def errorsToIds(
      compileErrors: NonEmptyList[CompilerErrMsg],
      mutatedFile: Source,
      mutants: MutantsWithId
  ) = {
    val statementToMutIdMap = mutants
      .map { mutant =>
        instrumenter.mutantToCase(mutant).structure -> mutant.id
      }
      .toVector
      .toMap

    val lineToMutantId: Map[Int, MutantId] = mutatedFile
      .collect {
        case node: Case if statementToMutIdMap.contains(node.structure) =>
          val mutId = statementToMutIdMap(node.structure)
          // +1 because scalameta uses zero-indexed line numbers
          (node.pos.startLine to node.pos.endLine).map(i => i + 1 -> mutId)
      }
      .flatten
      .toMap

    compileErrors.toList.mapFilter { err =>
      lineToMutantId.get(err.line).tupleRight(err)
    }.toMap
  }
}

final case class RollbackResult(newFiles: Vector[MutatedFile], compileErrors: MutantResultsPerFile)
