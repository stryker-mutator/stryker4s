package stryker4jvm.mutants.language

import scala.meta.Term
import scala.meta.Tree
import scala.meta.internal.trees.Origin
import scala.meta

import stryker4jvm.core.model.AST

class ScalaAST(term: Term) extends AST {
  def syntax: String = {
    term.syntax
  }

  override def equals(x: Any): Boolean = {
    if (x == null) {
      return false
    } else if (!(x.isInstanceOf[AST])) {
      return false
    }

    term == x
  }

  override def hashCode(): Int = {
    term.hashCode
  }
}
