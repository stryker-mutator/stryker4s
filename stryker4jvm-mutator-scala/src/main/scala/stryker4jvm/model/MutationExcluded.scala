package stryker4jvm.model

import stryker4jvm.core.model.IgnoredMutationReason

/** A mutation was excluded through user configuration
  */
final case object MutationExcluded extends IgnoredMutationReason {
  def explanation: String = "Mutation was excluded by user configuration"
}
