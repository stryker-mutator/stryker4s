package stryker4jvm.mutants.language

import scala.meta.Term
import scala.meta.Tree
import scala.meta.internal.trees.Origin
import scala.meta

class ScalaAST extends AST {
  def syntax: String = {
    "1"
  }
}

abstract class ASTScala extends Term with AST {

  override def syntax: String = {
    "2"
  }

  override def productElement(n: Int): Any = ???

  override def productArity: Int = ???

  override def privatePrototype: Tree = ???

  override def privateParent: Tree = ???

  override def privateOrigin: Origin = ???

  override def privateCopy(prototype: Tree, parent: Tree, destination: String, origin: Origin): Tree = ???

  override def productFields: List[String] = ???

  override def children: List[Tree] = ???

}
