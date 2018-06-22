package stryker4s.extensions.mutationtypes

import scala.meta.Lit

case object True extends LiteralMutation[Lit.Boolean] {
  override val tree: Lit.Boolean = Lit.Boolean(true)
}

case object False extends LiteralMutation[Lit.Boolean] {
  override val tree: Lit.Boolean = Lit.Boolean(false)
}
