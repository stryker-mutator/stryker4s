package stryker4s.mutants.applymutants

import scala.meta._

import stryker4s.log.Logger
import stryker4s.model.{Mutant, TransformedMutants}
import stryker4s.mutants.applymutants.ActiveMutationContext.ActiveMutationContext

class CoverageMatchBuilder(mutationContext: ActiveMutationContext)(implicit log: Logger)
    extends MatchBuilder(mutationContext) {

  // sbt-stryker4s-testrunner matches on Int instead of Option[Int]
  override def mutantToCase(mutant: Mutant): Case =
    super.buildCase(mutant.mutated, p"${mutant.id}")

  override def defaultCase(transformedMutant: TransformedMutants): Case =
    withCoverage(super.defaultCase(transformedMutant), transformedMutant.mutantStatements)

  private def withCoverage(caze: Case, mutants: List[Mutant]): Case = {
    val coverageStatement = mutants.map(mutant => q"_root_.stryker4s.coverage.coverMutant(${mutant.id})")
    val newBody = caze.body match {
      case b: Term.Block => coverageStatement ++ b.stats
      case other         => coverageStatement :+ other
    }
    caze.copy(body = Term.Block(newBody))
  }
}
