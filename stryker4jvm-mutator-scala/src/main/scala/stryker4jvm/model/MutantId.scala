package stryker4jvm.model

final case class MutantId(value: Int) extends AnyVal {
  override def toString(): String = value.toString
}
