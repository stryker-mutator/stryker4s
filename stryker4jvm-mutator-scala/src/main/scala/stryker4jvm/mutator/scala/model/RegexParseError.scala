package stryker4jvm.model

import stryker4jvm.core.model.IgnoredMutationReason

/** Weapon-regeX gave a failure when parsing a regular expression
  */
final case class RegexParseError(pattern: String, message: String) extends IgnoredMutationReason {
  def explanation: String =
    s"The Regex parser of weapon-regex couldn't parse this regex pattern: '$pattern'. Please report this issue at https://github.com/stryker-mutator/weapon-regex/issues. Inner error: $message"
}
