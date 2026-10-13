package stryker4s.plugin

import stryker4s.plugin.api.CustomMutator

/** Service-provider entry point for third-party mutators.
  *
  * Implementations must have a public no-argument constructor and be registered in
  * `META-INF/services/stryker4s.plugin.CustomMutatorPlugin`.
  */
trait CustomMutatorPlugin {
  // Fully qualified: unqualified `Seq` erases to `scala.collection.Seq` on Scala 2.12
  def mutators: scala.collection.immutable.Seq[CustomMutator]
}
