package stryker4s.mutants.tree

import cats.syntax.option.*
import stryker4s.mutants.applymutants.ActiveMutationContext
import stryker4s.mutants.applymutants.ActiveMutationContext.ActiveMutationContext

import scala.meta.*

final case class InstrumenterOptions(
    mutationContext: ActiveMutationContext,
    pattern: Int => Pat,
    condition: Option[DefaultMutationCondition]
)

object InstrumenterOptions {

  def sysContext(context: ActiveMutationContext) =
    InstrumenterOptions(
      context,
      pattern = i => Pat.Extract.After_4_6_0(Term.Name("Some"), Pat.ArgClause(List(Lit.String(i.toString())))),
      none
    )

  def testRunner = InstrumenterOptions(
    ActiveMutationContext.testRunner,
    pattern = Lit.Int(_),
    condition = Some(ids =>
      // _root_.stryker4s.coverage.coverMutant(ids*)
      Term.Apply.After_4_6_0(
        Term.Select(
          Term.Select(
            Term.Select(
              Term.Name("_root_"),
              Term.Name("stryker4s")
            ),
            Term.Name("coverage")
          ),
          Term.Name("coverMutant")
        ),
        Term.ArgClause(ids.map(Lit.Int(_)).toList)
      )
    )
  )
}
