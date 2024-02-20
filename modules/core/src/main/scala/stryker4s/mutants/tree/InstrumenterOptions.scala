package stryker4s.mutants.tree

import cats.syntax.option.*
import stryker4s.mutants.applymutants.ActiveMutationContext
import stryker4s.mutants.applymutants.ActiveMutationContext.ActiveMutationContext

import scala.meta.quasiquotes.*
import scala.meta.{Lit, Pat}

final case class InstrumenterOptions(
    mutationContext: ActiveMutationContext,
    pattern: Int => Pat,
    condition: Option[DefaultMutationCondition]
)

object InstrumenterOptions {

  def sysContext(context: ActiveMutationContext) =
    InstrumenterOptions(context, pattern = i => p"Some(${Lit.String(i.toString())})", none)

  def testRunner = InstrumenterOptions(
    ActiveMutationContext.testRunner,
    pattern = Lit.Int(_),
    condition = Some(ids => q"_root_.stryker4s.coverage.coverMutant(..${ids.map(Lit.Int(_)).toList})")
  )
}
