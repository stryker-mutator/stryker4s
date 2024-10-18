package stryker4s.model

import scalapb.TypeMapper

final case class MutantId(value: Int) extends AnyVal {
  override def toString(): String = value.toString
}

object MutantId {
  implicit val tm: TypeMapper[Int, MutantId] = TypeMapper[Int, MutantId](MutantId(_))(_.value)
}
