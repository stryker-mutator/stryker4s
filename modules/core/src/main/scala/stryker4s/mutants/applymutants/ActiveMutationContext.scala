package stryker4s.mutants.applymutants

import scala.meta.*

object ActiveMutationContext {
  type ActiveMutationContext = Term

  lazy val envVar: ActiveMutationContext = sysContext(Term.Name("env"))

  lazy val sysProps: ActiveMutationContext = sysContext(Term.Name("props"))

  // _root_.stryker4s.activeMutation
  lazy val testRunner: ActiveMutationContext =
    Term.Select(Term.Select(Term.Name("_root_"), Term.Name("stryker4s")), Term.Name("activeMutation"))

  // _root_.scala.$c.get("ACTIVE_MUTATION")
  private def sysContext(c: Term.Name): Term.Apply =
    Term.Apply(
      Term.Select(
        Term.Select(
          Term.Select(
            Term.Select(
              Term.Name("_root_"),
              Term.Name("scala")
            ),
            Term.Name("sys")
          ),
          c
        ),
        Term.Name("get")
      ),
      Term.ArgClause(List(Lit.String("ACTIVE_MUTATION")))
    )
}
