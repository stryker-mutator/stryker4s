package stryker4s.model

/** Reason why a mutator did not produce a mutant
  */
sealed trait IgnoredMutationReason

/** A mutation was excluded through user configuration
  */
final case class MutationExcluded() extends IgnoredMutationReason

/** Weapon-regeX gave a failure when parsing a regular expression
  */
final case class RegexParseError(original: String, message: String) extends IgnoredMutationReason
