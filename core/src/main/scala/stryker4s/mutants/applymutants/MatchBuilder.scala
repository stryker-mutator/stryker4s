package stryker4s.mutants.applymutants

import stryker4s.extension.TreeExtensions.{IsEqualExtension, TransformOnceExtension}
import stryker4s.extension.exception.UnableToBuildPatternMatchException
import stryker4s.log.Logger
import stryker4s.model.{Mutant, SourceTransformations, TransformedMutants}
import stryker4s.mutants.applymutants.ActiveMutationContext.ActiveMutationContext

import scala.meta.*
import scala.util.{Failure, Success}

class MatchBuilder(mutationContext: ActiveMutationContext)(implicit log: Logger) {
  def buildNewSource(transformedStatements: SourceTransformations): Tree = {
    val source = transformedStatements.source

    groupTransformedStatements(transformedStatements).foldLeft(source: Tree) { (rest, mutants) =>
      val origStatement = mutants.originalStatement

      var isTransformed = false
      rest transformOnce {
        case found if found.isEqual(origStatement) && found.pos == origStatement.pos =>
          isTransformed = true
          buildMatch(mutants)
      } match {
        case Success(value) if isTransformed => value
        case Success(value) =>
          log.warn(
            s"Failed to add mutation(s) ${mutants.mutantStatements.map(_.id).mkString(", ")} to new mutated code"
          )
          log.warn(
            s"The code that failed to mutate was: [$origStatement] at ${origStatement.pos.input}:${origStatement.pos.startLine + 1}:${origStatement.pos.startColumn + 1}"
          )
          log.warn("This mutation will likely show up as Survived")
          log.warn(
            "Please open an issue on github with sample code of the mutation that failed: https://github.com/stryker-mutator/stryker4s/issues/new"
          )
          value
        case Failure(exception) =>
          log.error(s"Failed to construct pattern match: original statement [$origStatement]")
          log.error(s"Failed mutation(s) ${mutants.mutantStatements.mkString(",")}.")
          log.error(
            s"at ${origStatement.pos.input}:${origStatement.pos.startLine + 1}:${origStatement.pos.startColumn + 1}"
          )
          log.error("This is likely an issue on Stryker4s's end, please enable debug logging and restart Stryker4s.")
          log.debug("Please open an issue on github: https://github.com/stryker-mutator/stryker4s/issues/new")
          log.debug("Please be so kind to copy the stacktrace into the issue", exception)

          throw UnableToBuildPatternMatchException()
      }
    }
  }

  def buildMatch(transformedMutant: TransformedMutants): Term.Match = {
    val cases: List[Case] = transformedMutant.mutantStatements.map(mutantToCase) :+ defaultCase(transformedMutant)

    q"($mutationContext match { ..case $cases })"
  }

  def mutantToCase(mutant: Mutant): Case =
    buildCase(mutant.mutated, p"Some(${mutant.id.globalId})")

  protected def defaultCase(transformedMutant: TransformedMutants): Case =
    buildCase(transformedMutant.originalStatement, p"_")

  protected def buildCase(expression: Term, pattern: Pat): Case = p"case $pattern => $expression"

  private def groupTransformedStatements(transformedStatements: SourceTransformations): Seq[TransformedMutants] = {
    transformedStatements.transformedStatements
      .groupBy(_.originalStatement)
      .map { case (key, transformedMutants) =>
        (key, transformedMutants.flatMap(transformedMutant => transformedMutant.mutantStatements))
      }
      .map { case (originalStatement, mutants) => TransformedMutants(originalStatement, mutants.toList) }
      .toSeq
      .sortBy(
        _.mutantStatements.head.id.globalId
      ) // Should be sorted so tree transformations are applied in order of discovery
  }
}
