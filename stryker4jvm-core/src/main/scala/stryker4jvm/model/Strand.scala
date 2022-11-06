package stryker4jvm.model

import scala.meta.*

sealed trait Strand[+T <: Tree] {
  def mutationName: String
}
