package stryker4s.plugin.api

/** Reason why a mutator did not produce a mutant
  */
sealed trait IgnoredMutationReason {
  def explanation: String
}

/** A mutation was excluded through user configuration
  */
case object MutationExcluded extends IgnoredMutationReason {
  def explanation: String = "Mutation was excluded by user configuration"
}

/** Weapon-regeX gave a failure when parsing a regular expression
  */
final case class RegexParseError(pattern: String, message: String) extends IgnoredMutationReason {
  def explanation: String =
    s"The Regex parser of weapon-regex couldn't parse this regex pattern: '$pattern'. Please report this issue at https://github.com/stryker-mutator/weapon-regex/issues. Inner error: $message"
}

final case class NoRegexMutationsFound(pattern: String) extends IgnoredMutationReason {
  def explanation: String = s"No mutations found in this regular expression: '$pattern'"
}

/** A mutation could not be placed inside its statement (see `PlaceableTree.substitute`)
  */
final case class SubstitutionFailed(original: String, statement: String, message: String)
    extends IgnoredMutationReason {
  def explanation: String = s"Could not substitute '$original' in '$statement': $message"
}
