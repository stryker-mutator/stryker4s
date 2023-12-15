package stryker4s.testrunner.api

import scalapb.TypeMapper

final case class TestFileId(value: Int) extends AnyVal {
  override def toString(): String = value.toString
}

object TestFileId {
  implicit val tm: TypeMapper[Int, TestFileId] = TypeMapper[Int, TestFileId](TestFileId(_))(_.value)
}
