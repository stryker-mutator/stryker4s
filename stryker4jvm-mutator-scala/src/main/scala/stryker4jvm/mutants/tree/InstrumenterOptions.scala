package stryker4s.mutants.tree

import stryker4s.mutants.applymutants.ActiveMutationContext.ActiveMutationContext
import stryker4s.mutants.applymutants.ActiveMutationContext
import scala.meta.{Lit, Pat}
import scala.meta.quasiquotes.*

final case class InstrumenterOptions private (
    mutationContext: ActiveMutationContext,
    pattern: Int => Pat,
    condition: Option[DefaultMutationCondition]
)

object InstrumenterOptions {

  def sysContext(context: ActiveMutationContext) =
    InstrumenterOptions(context, pattern = i => p"Some(${Lit.String(i.toString())})", None)

  def testRunner = InstrumenterOptions(
    ActiveMutationContext.testRunner,
    pattern = i => p"$i",
    condition = Some(ids => q"_root_.stryker4s.coverage.coverMutant(..${ids.map(Lit.Int(_)).toList})")
  )
}
