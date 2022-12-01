package stryker4jvm.mutator.scala.model

final case class MutantId(value: Int) extends AnyVal {
  override def toString(): String = value.toString
}
