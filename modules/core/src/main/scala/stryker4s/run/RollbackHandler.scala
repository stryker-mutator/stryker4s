package stryker4s.run

import cats.data.Ior.Both
import cats.data.{Ior, NonEmptyList}
import cats.syntax.all.*
import fansi.Color
import mutationtesting.MutantStatus
import stryker4s.log.Logger
import stryker4s.model.CompilerErrMsg.*
import stryker4s.model.{CompilerErrMsg, MutantResultsPerFile, MutatedFile}
import stryker4s.mutants.tree.MutantInstrumenter

import scala.meta.Source
import scala.meta.parsers.*

class RollbackHandler(instrumenter: MutantInstrumenter)(implicit log: Logger) {

  def rollbackFiles(
      errors: NonEmptyList[CompilerErrMsg],
      allFiles: Vector[MutatedFile]
  ): Either[NonEmptyList[CompilerErrMsg], RollbackResult] = {

    log.info(
      s"${Color.Red(errors.size.toString())} mutant(s) gave a compiler error. They will be marked as such in the report."
    )

    // Find all files that have a compile error
    val compileErrorFiles = allFiles.mapFilter { mutatedFile =>
      val errorsForFile = NonEmptyList.fromList(
        errors.filter(err => mutatedFile.fileOrigin.toString.endsWith(err.path))
      )
      errorsForFile.tupleLeft(mutatedFile)
    }
    // Go through filesWithErrors, parse the mutatedFile again and remove mutant case statement that have a compile error inside
    val filesWithRemovedErrors = compileErrorFiles
      .traverse { case (mutatedFile, errors) =>
        val parsed = mutatedFile.mutatedSource.syntax
          .parse[Source]
          .get // Should always pass as we already parsed it once

        log.debug(
          s"Removing ${errors.size} mutants with compile errors from ${mutatedFile.fileOrigin}: ${errors.map(_.show).mkString_("'", "', '", "'")}"
        )
        val treeWithoutErrors = parsed.transform(instrumenter.attemptRemoveMutant(errors))
        val (errorsWithoutIds, compileErrorMutantIds) = instrumenter.mutantIdsForCompileErrors(parsed, errors)

        // Split into mutants that have a compile error and mutants that don't
        val splitMutantsWithErrors = mutatedFile.mutants
          .nonEmptyPartition { mutant =>
            compileErrorMutantIds
              .get(mutant.id)
              .map(error => mutant.toMutantResult(MutantStatus.CompileError, description = error.show.some))
              .toRight(mutant)
          }
          .map(mutatedFile.fileOrigin -> _.toList.toVector)

        splitMutantsWithErrors match {
          // Some mutants were removed, some still remain
          case Both(mutants, results) =>
            // If not all errors were removed, we can't continue (return a Left)
            errorsWithoutIds.toLeft(
              mutatedFile.copy(mutatedSource = treeWithoutErrors, mutants = mutants.toNev).asRight -> results
            )
          // All mutants were removed
          case Ior.Right(results) => (mutatedFile.fileOrigin.asLeft, results).asRight
          // No mutants were removed. Something probably went wrong
          case Ior.Left(_) =>
            log.error(
              s"No mutants were removed in ${mutatedFile.fileOrigin} even though there were ${errors.size} compile errors."
            )
            errors.asLeft
        }
      }
      .map { files =>
        val ((pathsToFilterOut, fixedFiles), compileErrors) = files.unzip
          .bimap(_.partitionEither(identity), _.toMap)
        val allNewFiles =
          fixedFiles ++ allFiles
            .filterNot(f => fixedFiles.exists(_.fileOrigin == f.fileOrigin) || pathsToFilterOut.contains(f.fileOrigin))
        RollbackResult(allNewFiles, compileErrors)
      }

    filesWithRemovedErrors
  }

}

final case class RollbackResult(newFiles: Seq[MutatedFile], compileErrors: MutantResultsPerFile)
