package stryker4s.pluginapi

import scala.meta.Term

/** A mutated version of a piece of code, together with metadata describing the mutation.
  *
  * This is the result type a [[stryker4s.pluginapi.CustomMutator]] produces for each place it finds a mutation.
  */
final case class MutatedCode(mutatedStatement: Term, metadata: MutantMetadata)
