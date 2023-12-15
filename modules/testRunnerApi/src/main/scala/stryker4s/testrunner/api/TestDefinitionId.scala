package stryker4s.testrunner.api

import scalapb.TypeMapper

final case class TestDefinitionId(value: Int) extends AnyVal {
  override def toString(): String = value.toString
}

object TestDefinitionId {
  implicit val tm: TypeMapper[Int, TestDefinitionId] = TypeMapper[Int, TestDefinitionId](TestDefinitionId(_))(_.value)
}
