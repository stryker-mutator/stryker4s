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
    val mutants = files
      .map { file =>
        val mutationsInSource = findMutants(file)
        val transformed = transformStatements(mutationsInSource)
        val builtTree = buildMatches(transformed)

        MutatedFile(file, builtTree, mutationsInSource.mutants)
      }
      .filter(_.mutants.nonEmpty)

    info(s"Found ${mutants.size} of ${files.size} file(s) to be mutated.")
    info(
      s"${mutants.flatMap(_.mutants).flatMap(_.mutants).size} Mutant(s) generated")
    mutants
  }

  /** Step 1: Find mutants in the found files
    */
  private def findMutants(file: File): MutationsInSource =
    mutantFinder.mutantsInFile(file)

  /** Step 2: transform the statements of the found mutants (preparation of building pattern matches)
    */
  private def transformStatements(
      mutants: MutationsInSource): SourceTransformations =
    transformer.transformSource(mutants.source, mutants.mutants)

  /** Step 3: Build pattern matches from transformed trees
    */
  private def buildMatches(
      transformedMutantsInSource: SourceTransformations): Tree =
    matchBuilder.buildNewSource(transformedMutantsInSource)
}
