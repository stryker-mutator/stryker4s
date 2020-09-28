package stryker4s.mutants.applymutants
import scala.meta._

object ActiveMutationContext {
  type ActiveMutationContext = Term.Apply

  private lazy val activeMutationEnv = Lit.String("ACTIVE_MUTATION")
  lazy val envVar: ActiveMutationContext = q"_root_.scala.sys.env.get($activeMutationEnv)"
  lazy val sysProps: ActiveMutationContext = q"_root_.scala.sys.props.get($activeMutationEnv)"
}
