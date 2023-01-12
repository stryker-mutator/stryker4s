package stryker4jvm.mutator.scala

import scala.meta.*
import cats.data.NonEmptyList
import ActiveMutationContext.ActiveMutationContext

object ActiveMutationContext {
  type ActiveMutationContext = Term

  lazy val envVar: ActiveMutationContext = sysContext(q"env")

  lazy val sysProps: ActiveMutationContext = sysContext(q"props")

  lazy val testRunner: ActiveMutationContext = q"_root_.stryker4s.activeMutation"

  private def sysContext(c: Term.Name): Term.Apply = q"_root_.scala.sys.$c.get(${Lit.String("ACTIVE_MUTATION")})"
}

final case class ScalaInstrumenterOptions private (
    mutationContext: ActiveMutationContext,
    pattern: Int => Pat,
    condition: Option[(NonEmptyList[Int]) => Term]
)

object ScalaInstrumenterOptions {

  def sysContext(context: ActiveMutationContext) =
    ScalaInstrumenterOptions(context, pattern = i => p"Some(${Lit.String(i.toString())})", None)

  def testRunner = ScalaInstrumenterOptions(
    ActiveMutationContext.testRunner,
    pattern = i => p"$i",
    condition = Some(ids => q"_root_.stryker4s.coverage.coverMutant(..${ids.map(Lit.Int(_)).toList})")
  )
}
