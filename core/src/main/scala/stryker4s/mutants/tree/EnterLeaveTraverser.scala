package stryker4s.mutants.tree

import scala.meta.Tree
import scala.meta.transversers.SimpleTraverser

class EnterLeaveTraverser(onEnter: Tree => Unit, onLeave: Tree => Unit) extends SimpleTraverser {
  override def apply(tree: Tree): Unit = {
    onEnter(tree)
    super.apply(tree)
    onLeave(tree)
  }
}
