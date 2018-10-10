package stryker4s.mutants.applymutants

import grizzled.slf4j.Logging
import stryker4s.extensions.TreeExtensions.ImplicitTreeExtensions
import stryker4s.extensions.exceptions.UnableToBuildPatternMatchException
import stryker4s.model.{Mutant, SourceTransformations, TransformedMutants}

import scala.meta._
import scala.meta.contrib.implicits.Equality.XtensionTreeEquality
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
            error(s"Failed to construct pattern match: original statement [$origStatement]")
            error(s"Failed mutation(s) ${mutant.mutantStatements.mkString(",")}.")
            error(s"at ${origStatement.pos.input}:${origStatement.pos.startLine + 1}:${origStatement.pos.startColumn + 1}")
            error("This is likely an issue on Stryker4s's end, please enable debug logging and restart Stryker4s.")

            debug("Please open an issue on github: https://github.com/stryker-mutator/stryker4s/issues/new")
            debug("Please be so kind to copy the stacktrace into the issue", exception)

            throw UnableToBuildPatternMatchException()
        }
      }
  }

  def buildMatch(transformedMutant: TransformedMutants): Term.Match = {
    val cases: List[Case] = transformedMutant.mutantStatements
      .map(mutantToCase) :+ defaultCase(transformedMutant)

    val activeMutationEnv = Lit.String("ACTIVE_MUTATION")

    q"(sys.props.get($activeMutationEnv).orElse(sys.env.get($activeMutationEnv)) match { ..case $cases })"
  }

  private def mutantToCase(mutant: Mutant): Case =
    buildCase(mutant.mutated, p"Some(${Lit.String(mutant.id.toString)})")

  private def defaultCase(transformedMutant: TransformedMutants): Case =
    buildCase(transformedMutant.originalStatement, p"_")

  private def buildCase(expression: Term, pattern: Pat): Case =
    p"case $pattern => $expression"

}
