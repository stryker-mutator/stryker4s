package stryker4s.mutants.applymutants

import scala.meta._

import stryker4s.log.Logger
import stryker4s.model.Mutant
import stryker4s.mutants.applymutants.ActiveMutationContext.ActiveMutationContext

class CoverageMatchBuilder(mutationContext: ActiveMutationContext)(implicit log: Logger)
    extends MatchBuilder(mutationContext) {

  override def mutantToCase(mutant: Mutant): Case = withCoverage(super.mutantToCase(mutant), mutant.id)

  private def withCoverage(caze: Case, mutantId: Int): Case = {
    val coverageStatement = q"_root_.stryker4s.coverage.coverMutant($mutantId)"
    val newBody = caze.body match {
      case b: Term.Block => coverageStatement +: b.stats
      case other         => List(coverageStatement, other)
    }
    caze.copy(body = Term.Block(newBody))
  }
}
