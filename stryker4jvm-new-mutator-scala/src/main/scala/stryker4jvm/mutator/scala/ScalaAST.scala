package stryker4jvm.mutator.scala

import stryker4jvm.core.model.AST
import scala.meta.Source
import scala.meta.Tree
import scala.meta.Term

class ScalaAST(val source: Source = null, val tree: Tree = null, val term: Term = null) extends AST {
  override def syntax(): String = {
    if (source != null) {
      return source.syntax
    }

    if (tree != null) {
      return tree.syntax
    }

    "Undefined"
  }
  override def hashCode(): Int = {
    if (source != null) {
      return source.hashCode()
    }

    if (tree != null) {
      return tree.hashCode()
    }

    -1
  }
  override def equals(obj: Any): Boolean = {
    if (obj == null) {
      return false
    }

    obj match {
      case _: Source => obj == source
      case _: Tree   => obj == tree
      case _         => false
    }

  }
}
