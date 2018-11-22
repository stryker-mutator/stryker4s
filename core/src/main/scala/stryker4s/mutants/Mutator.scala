package stryker4s.mutants

import better.files.File
import grizzled.slf4j.Logging
import stryker4s.model.{MutatedFile, MutationsInSource, SourceTransformations}
import stryker4s.mutants.applymutants.{MatchBuilder, StatementTransformer}
import stryker4s.mutants.findmutants.MutantFinder

import scala.meta.Tree

class Mutator(mutantFinder: MutantFinder,
              transformer: StatementTransformer,
              matchBuilder: MatchBuilder)
    extends Logging {

  def mutate(files: Iterable[File]): Iterable[MutatedFile] = {
    val mutatedFiles = files
      .map { file =>
        val mutationsInSource = findMutants(file)
        val transformed = transformStatements(mutationsInSource)
        val builtTree = buildMatches(transformed)

        MutatedFile(file, builtTree, mutationsInSource.mutants, mutationsInSource.excluded)
      }
      .filterNot(mutatedFile => mutatedFile.mutants.isEmpty && mutatedFile.excludedMutants.isEmpty)

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
    val excludedMutants = mutatedFiles.flatMap(_.excludedMutants).size

    info(s"Found ${mutatedFiles.size} of $totalAmountOfFiles file(s) to be mutated.")
    info(s"${includedMutants + excludedMutants} Mutant(s) generated.")
    if (excludedMutants > 0) {
      info(s"Of which $excludedMutants Mutant(s) are excluded.")
    }
  }
}
