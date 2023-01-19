package stryker4jvm.testutil

import stryker4jvm.core.model.AST

/** Incomplete implementation of a scala AST. Should not be used in maps...
  */
class TestAST(val tree: scala.meta.Tree) extends AST {

  override def syntax(): String = tree.syntax

  override def equals(obj: Any): Boolean = ???

  override def hashCode(): Int = ???
}
