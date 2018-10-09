package stryker4s.mutants.applymutants

import grizzled.slf4j.Logging
import stryker4s.extensions.TreeExtensions.ImplicitTreeExtensions
import stryker4s.model.{Mutant, SourceTransformations, TransformedMutants}

import scala.meta.contrib.implicits.Equality.XtensionTreeEquality
import scala.meta.{Case, Lit, Pat, Term, Tree, _}
import scala.util.{Failure, Success}

class MatchBuilder extends Logging {

  def buildNewSource(transformedStatements: SourceTransformations): Tree = {
    val source = transformedStatements.source
    val transformedMutants = transformedStatements.transformedStatements

    transformedMutants
      .foldLeft(source: Tree) { (rest, mutant) =>
        val origStatement = mutant.originalStatement

        rest transformOnce {
          case found if found.isEqual(origStatement) && found.pos == origStatement.pos =>
            buildMatch(mutant)
        } match {
          case Success(value) => value
          case Failure(exception) =>
            error(
              s"Failed to construct pattern match: original statement [$origStatement] " +
                s"on line [${origStatement.pos.endLine}] for file [${origStatement.pos.input}].)")
            error(s"Failed mutation(s) ${mutant.mutantStatements.mkString(",")}.")
            debug(exception.getMessage)

            throw exception
        }
      }
  }

  def buildMatch(transformedMutant: TransformedMutants): Term.Match = {
    val cases: List[Case] = transformedMutant.mutantStatements
      .map(mutantToCase) :+ defaultCase(transformedMutant)

    val activeMutationEnv = Lit.String("ACTIVE_MUTATION")

    q"(sys.env.get($activeMutationEnv) match { ..case $cases })"
  }

  private def mutantToCase(mutant: Mutant): Case =
    buildCase(mutant.mutated, p"Some(${Lit.String(mutant.id.toString)})")

  private def defaultCase(transformedMutant: TransformedMutants): Case =
    buildCase(transformedMutant.originalStatement, p"_")

  private def buildCase(expression: Term, pattern: Pat): Case =
    p"case $pattern => $expression"

}
