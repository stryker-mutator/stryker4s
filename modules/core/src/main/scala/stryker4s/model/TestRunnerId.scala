package stryker4s.model

final case class TestRunnerId(value: Int) extends AnyVal {
  override def toString(): String = value.toString
}
