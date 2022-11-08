package stryker4jvm.model

/** Reason why a mutator did not produce a mutant
  */
sealed trait IgnoredMutationReason {
  def explanation: String
}

/** A mutation was excluded through user configuration
  */
final case object MutationExcluded extends IgnoredMutationReason {
  def explanation: String = "Mutation was excluded by user configuration"
}

/** Weapon-regeX gave a failure when parsing a regular expression
  */
final case class RegexParseError(pattern: String, message: String) extends IgnoredMutationReason {
  def explanation: String =
    s"The Regex parser of weapon-regex couldn't parse this regex pattern: '$pattern'. Please report this issue at https://github.com/stryker-mutator/weapon-regex/issues. Inner error: $message"
}
