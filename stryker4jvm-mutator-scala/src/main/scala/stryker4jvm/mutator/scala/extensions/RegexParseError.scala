package stryker4jvm.mutator.scala.extensions
import stryker4jvm.core.model.IgnoredMutationReason

final case class RegexParseError(pattern: String, message: String) extends IgnoredMutationReason {

  override def explanation(): String =
    s"The Regex parser of weapon-regex couldn't parse this regex pattern: '$pattern'. Please report this issue at https://github.com/stryker-mutator/weapon-regex/issues. Inner error: $message"

}
