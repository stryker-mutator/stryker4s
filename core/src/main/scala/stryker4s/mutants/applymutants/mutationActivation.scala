package stryker4s.mutants.applymutants
import scala.meta._

sealed trait ActiveMutationContext {
  val name: Term.Name
}

object Env extends ActiveMutationContext {
  override val name: Term.Name = q"env"
}

object Props extends ActiveMutationContext {
  override val name: Term.Name = q"props"
}
