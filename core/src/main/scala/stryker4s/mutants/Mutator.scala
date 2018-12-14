package stryker4s.mutants

import better.files.File
import grizzled.slf4j.Logging
import stryker4s.model.{MutatedFile, MutationsInSource, SourceTransformations}
import stryker4s.mutants.applymutants.{MatchBuilder, StatementTransformer}
import stryker4s.mutants.findmutants.MutantFinder

import scala.meta.{Term, Tree}


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
        val interpolatedFix = wrapInterpolations(builtTree)
        MutatedFile(file.path, interpolatedFix, mutationsInSource.mutants, mutationsInSource.excluded)
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

  private def logMutationResult(mutatedFiles: Iterable[MutatedFile],
                                totalAmountOfFiles: Int): Unit = {
    val includedMutants = mutatedFiles.flatMap(_.mutants).size
    val excludedMutants = mutatedFiles.map(_.excludedMutants).sum

    info(s"Found ${mutatedFiles.size} of $totalAmountOfFiles file(s) to be mutated.")
    info(s"${includedMutants + excludedMutants} Mutant(s) generated.")
    if (excludedMutants > 0) {
      info(s"Of which $excludedMutants Mutant(s) are excluded.")
    }
  }

  /** Wrap a `Term.Name` args of a `Term.Interpolate` args in a `Term.Block` to work around a bug in Scalameta: https://github.com/scalameta/scalameta/issues/1792
    */
  private def wrapInterpolations(builtTree: Tree) = builtTree transform {
    case Term.Interpolate(prefix, parts, args) =>
      Term.Interpolate(prefix, parts, args map {
        case t: Term.Name => Term.Block(List(t))
        case other => other
      })
  }
}
