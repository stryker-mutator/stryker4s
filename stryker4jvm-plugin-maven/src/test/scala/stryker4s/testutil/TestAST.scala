package stryker4s.testutil

import stryker4jvm.core.model.AST

class TestAST(val tree: scala.meta.Tree) extends AST {

  override def syntax(): String = tree.syntax

  override def equals(obj: Any): Boolean = false

  override def hashCode(): Int = tree.hashCode()
}
