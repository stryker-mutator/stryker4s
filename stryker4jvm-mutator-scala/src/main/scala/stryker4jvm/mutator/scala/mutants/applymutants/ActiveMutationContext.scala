package stryker4jvm.mutator.scala.mutants.applymutants

import scala.meta.*

object ActiveMutationContext {
  type ActiveMutationContext = Term

  lazy val envVar: ActiveMutationContext = sysContext(q"env")

  lazy val sysProps: ActiveMutationContext = sysContext(q"props")

  lazy val testRunner: ActiveMutationContext = q"_root_.stryker4s.activeMutation"

  private def sysContext(c: Term.Name): Term.Apply = q"_root_.scala.sys.$c.get(${Lit.String("ACTIVE_MUTATION")})"
}
