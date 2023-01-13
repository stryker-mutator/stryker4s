package stryker4jvm.mutator.scala

import stryker4jvm.core.model.AST
import scala.meta.Tree

class ScalaAST(val value: Tree = null) extends AST {
  override def syntax(): String = {
    if (value != null) {
      return value.syntax
    }

    "Undefined"
  }
  override def hashCode(): Int = {
    if (value != null) {
      return value.hashCode()
    }

    -1
  }
  override def equals(obj: Any): Boolean = {
    if (obj == null) {
      println("OBJ NULL");
      return false
    }

    obj match {
      case _: Tree =>
        obj == value
      case ast: ScalaAST =>
        if (ast.value != null) {
          ast.value == value
        } else {
          false
        }
      case _ => false
    }

  }
}
