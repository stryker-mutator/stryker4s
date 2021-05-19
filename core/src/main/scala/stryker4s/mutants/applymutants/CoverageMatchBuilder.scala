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

  /** Call coverage in a place that's always safe to call: the 'if'-statement of the default match of the mutation switch. `coverMutant` always returns true
    */
  private def withCoverage(caze: Case, mutants: List[Mutant]): Case = {
    val coverageCond = q"_root_.stryker4s.coverage.coverMutant(..${mutants.map(_.id).map(Lit.Int(_))})"
    caze.copy(cond = Some(coverageCond))
  }
}
