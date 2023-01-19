package stryker4jvm.mutator.scala

import stryker4jvm.core.model.AST
import scala.meta.Tree

/** A class that is used to communicate with the rest of stryker4jvm
  *
  * @param value
  *   A value of type [[scala.meta.Tree]], this value is used in the rest of the scala mutator
  */
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

  /** Method to compare an object to the `value`
    *
    * @param obj
    *   Can be anything, but only objects of type [[scala.meta.Tree]] and [[ScalaAST]] can be used in comparison
    * @return
    *   Boolean value that indicates if the given object is the same as `value`
    */
  override def equals(obj: Any): Boolean = {
    if (obj == null) {
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
