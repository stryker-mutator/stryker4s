package stryker4s.mutants

import better.files.File
import grizzled.slf4j.Logging
import stryker4s.model.{MutatedFile, MutationsInSource, SourceTransformations}
import stryker4s.mutants.applymutants.{MatchBuilder, StatementTransformer}
import stryker4s.mutants.findmutants.MutantFinder

import scala.meta.Tree

class Mutator(mutantFinder: MutantFinder, transformer: StatementTransformer, matchBuilder: MatchBuilder)
    extends Logging {

  def mutate(files: Iterable[File]): Iterable[MutatedFile] = {
    val mutatedFiles = files
      .map { file =>
        val mutationsInSource = findMutants(file)
        val transformed = transformStatements(mutationsInSource)
        val builtTree = buildMatches(transformed)

        MutatedFile(file, builtTree, mutationsInSource.mutants, mutationsInSource.excluded)
      }
      .filterNot(mutatedFile => mutatedFile.mutants.isEmpty && mutatedFile.excludedMutants == 0)

    logMutationResult(mutatedFiles, files.size)

    mutatedFiles
  }

  /** Step 1: Find mutants in the found files
    */
  private def findMutants(file: File): MutationsInSource = mutantFinder.mutantsInFile(file)

  /** Step 2: transform the statements of the found mutants (preparation of building pattern matches)
    */
  private def transformStatements(mutants: MutationsInSource): SourceTransformations =
    transformer.transformSource(mutants.source, mutants.mutants)

  /** Step 3: Build pattern matches from transformed trees
    */
  private def buildMatches(transformedMutantsInSource: SourceTransformations): Tree =
    matchBuilder.buildNewSource(transformedMutantsInSource)

  private def logMutationResult(mutatedFiles: Iterable[MutatedFile], totalAmountOfFiles: Int): Unit = {
    val includedMutants = mutatedFiles.flatMap(_.mutants).size
    val excludedMutants = mutatedFiles.map(_.excludedMutants).sum
    val totalMutants = includedMutants + excludedMutants

    info(s"Found ${mutatedFiles.size} of $totalAmountOfFiles file(s) to be mutated.")
    info(s"$totalMutants Mutant(s) generated.${if (excludedMutants > 0)
      s" Of which $excludedMutants Mutant(s) are excluded."
    else ""}")

    if (totalAmountOfFiles == 0) {
      warn(s"No files marked to be mutated. ${dryRunText("mutate")}")
    } else if (includedMutants == 0 && excludedMutants > 0) {
      warn(s"All found mutations are excluded. ${dryRunText("excluded-mutations")}")
    } else if (totalMutants == 0) {
      info("Files to be mutated are found, but no mutations were found in those files.")
      info("If this is not intended, please check your configuration and try again.")
    }

    def dryRunText(configProperty: String): String =
      s"""Stryker4s will perform a dry-run without actually mutating anything.
         |You can configure the `$configProperty` property in your configuration""".stripMargin
  }

}
