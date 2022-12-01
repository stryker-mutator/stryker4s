package stryker4jvm.mutator.scala.mutants.language

import scala.meta
import stryker4jvm.core.model.AST

class ScalaAST(val term: meta.Term = null, val source: meta.Source = null) extends AST {

  def syntax: String = {
    if (term != null) {
      return term.syntax
    }

    if (source != null) {
      return source.syntax
    }

    "No term or source!"
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
    if (term != null) {
      return term.hashCode()
    }

    if (source != null) {
      return source.hashCode()
    }

    -1
  }
}
