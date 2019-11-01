package stryker4s.mutants.applymutants
import scala.meta._

object ActiveMutationContext {
  type ActiveMutationContext = Term.Name

  val envVar: ActiveMutationContext = q"env"
  val sysProps: ActiveMutationContext = q"props"
}
