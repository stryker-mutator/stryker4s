package stryker4s.model

import fs2.io.file.Path

import scala.meta._

final case class MutatedFile(
    fileOrigin: Path,
    tree: Tree,
    mutants: Seq[Mutant],
    mutationStatements: Seq[(MutantId, Tree)],
    excludedMutants: Int
) {

  def mutatedSource: String = {
    tree.syntax
  }

  //Returns a map of line numbers to the mutant on that line
  //Contains only mutated lines, and the same mutant can stretch over multiple multiple lines
  //This logic is not very fast, because it has to search the entire tree, that's why it's lazy
  lazy val mutantLineNumbers: Map[Int, MutantId] = {
    val statementToMutIdMap = mutationStatements.map { case (mutantId, mutationStatement) =>
      mutationStatement.structure -> mutantId
    }.toMap

    mutatedSource
      .parse[Stat] //Parse as a standalone statement, used in unit tests and conceivable in some real code
      .orElse(mutatedSource.parse[Source]) //Parse as a complete scala source file
      .get //If both failed something has gone very badly wrong, give up
      .collect {
        case node if statementToMutIdMap.contains(node.structure) =>
          val mutId = statementToMutIdMap(node.structure)
          //+1 because scalameta uses zero-indexed line numbers
          (node.pos.startLine to node.pos.endLine).map(i => i + 1 -> mutId)
      }
      .flatten
      .toMap
  }
}
