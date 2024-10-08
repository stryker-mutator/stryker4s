package stryker4s.config

final case class ExcludedMutation(value: String) extends AnyVal {
  override def toString: String = value
}
