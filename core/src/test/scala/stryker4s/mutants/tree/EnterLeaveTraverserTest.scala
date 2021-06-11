package stryker4s.mutants.tree

import stryker4s.testutil.Stryker4sSuite

import scala.collection.mutable.Buffer
import scala.meta.*

class EnterLeaveTraverserTest extends Stryker4sSuite {

  it("should call enter breadth-first") {
    val called = Buffer.empty[Tree]
    val append: Tree => Unit = called += _
    val tree = q"def foo = 4 + 2"
    val traverser = new EnterLeaveTraverser(append, _ => ())

    traverser(tree)

    called.toList.map(_.syntax) shouldBe List("def foo = 4 + 2", "foo", "4 + 2", "4", "+", "2")
  }

  it("should call onLeave depth-first") {
    val called = Buffer.empty[Tree]
    val append: Tree => Unit = called += _
    val tree = q"def foo = 4 + 2"
    val traverser = new EnterLeaveTraverser(_ => (), append)

    traverser(tree)

    called.toList.map(_.syntax) shouldBe List("foo", "4", "+", "2", "4 + 2", "def foo = 4 + 2")
  }

}
