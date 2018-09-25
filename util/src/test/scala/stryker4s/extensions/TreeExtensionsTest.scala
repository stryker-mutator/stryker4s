package stryker4s.extensions

import stryker4s.Stryker4sSuite
import stryker4s.extensions.TreeExtensions._
import stryker4s.scalatest.TreeEquality

import scala.meta._

class TreeExtensionsTest extends Stryker4sSuite with TreeEquality {
  describe("topStatement") {
    it("should return top statement in a simple statement") {
      val tree = q"x.times(2)"
      val subTree = tree.find(q"times").value

      val result = subTree.topStatement()

      subTree should equal(q"times")
      result should be theSameInstanceAs tree
    }

    it("should be top of statement in def") {
      val tree = q"def foo(age: Int) = age.equals(18)"
      val subTree = tree.find(q"equals").value

      val result = subTree.topStatement()

      subTree should equal(q"equals")
      result should equal(q"age.equals(18)")
    }

    it("should return top statement on infix statement") {
      val tree = q"def foo(age: Int) = age equals 18"
      val subTree = tree.find(q"equals").value

      val result = subTree.topStatement()

      subTree should equal(q"equals")
      result should equal(q"age equals 18")
    }

    it("should return top statement on multiple calls") {
      val tree = q"def foo(list: List[Int]) = list.map(_ * 2).filter(_ >= 2).isEmpty"
      val subTree = tree.find(q">=").value

      val result = subTree.topStatement()

      result should equal(q"list.map(_ * 2).filter(_ >= 2).isEmpty")
    }

    it("should return top statement on list creation with method calls") {
      val tree = q"def foo() = List(1, 2, 3).filter(_ >= 2).isEmpty"
      val subTree = tree.find(q">=").value

      val result = subTree.topStatement()

      result should equal(q"List(1, 2, 3).filter(_ >= 2).isEmpty")
    }

    it("should return top statement on multiple calls when mutation is last call") {
      val tree = q"def foo(list: List[Int]) = list.map(_ * 2).filter(_ >= 2).isEmpty"
      val subTree = tree.find(q"isEmpty").value

      val result = subTree.topStatement()

      result should equal(q"list.map(_ * 2).filter(_ >= 2).isEmpty")
    }

    it("should return top statement in a bigger def") {
      val tree =
        q"""def foo(list: List[Int], otherList: List[Int]) = {
        val firstResult = list
          .filter(_ % 2 == 0)
          .map(_ * 5)
          .reverse
        val secondResult = otherList
          .filter(_ >= 5)
          .map(_ * 3)
          .drop(5)
        (firstResult, secondResult)
      }"""
      val subTree = tree.find(q"_ * 3").value

      val result = subTree.topStatement()

      val expected =
        q"""otherList
          .filter(_ >= 5)
          .map(_ * 3)
          .drop(5)
         """
      result should equal(expected)
    }

    it("should return same statement of def in def with single statement") {
      val tree = q"def four: Int = 4"
      val subTree = tree.find(q"4").value

      val result = subTree.topStatement()

      result should equal(q"4")
    }

    it("should return same statement when topStatement is called twice") {
      val tree = q"def four: Boolean = x >= 4"
      val subTree = tree.find(q">=").value

      val result = subTree
        .topStatement()
        .topStatement()

      result should equal(q"x >= 4")
    }

    it("should return whole statement with && and || operator") {
      val tree = q"def four(x: Int): Boolean = x >= 4 && x < 10 || x <= 0"
      val subTree = tree.find(q"<=").value

      val result = subTree.topStatement()

      result should equal(q"x >= 4 && x < 10 || x <= 0")
    }

    it("should return whole statement on infix inside postfix statement") {
      val tree = q"def foo = Math.square(2 * 5)"
      val subTree = tree.find(q"*").value

      val result = subTree.topStatement()

      result should equal(q"Math.square(2 * 5)")
    }

    it("should not include if statement in top") {
      val tree = q"def foo(x: Int) = if(x > 5) x > 10"
      val subTree = tree.find(q"10").value

      val result = subTree.topStatement()

      result should equal(q"x > 10")
    }

    it("should not include if statement if expression is in the if statement") {
      val tree = q"def foo(x: Int) = if(x >= 5) x > 10"
      val subTree = tree.find(q">=").value

      val result = subTree.topStatement()

      result should equal(q"x >= 5")
    }

    it("should include new operator") {
      val tree = q"def foo = new Bar(4).filter(_ >= 3)"
      val subTree = tree.find(q">=").value

      val result = subTree.topStatement()

      result should equal(q"new Bar(4).filter(_ >= 3)")
    }

    it("should include entire statement with && statement") {
      val tree = q"def foo = a == b && b == c"
      val subTree = tree.find(q"&&").value

      val result = subTree.topStatement()

      result should equal(q"a == b && b == c")
    }

    it("should include entire statement when && is not symmetrical on left") {
      val tree = q"def foo = a && b == c"
      val subTree = tree.find(q"&&").value

      val result = subTree.topStatement()

      result should equal(q"a && b == c")
    }

    it("should include entire statement when && is not symmetrical on right") {
      val tree = q"def foo = a == b && c"
      val subTree = tree.find(q"&&").value

      val result = subTree.topStatement()

      result should equal(q"a == b && c")
    }

    it("should include generic type") {
      val tree = q"def foo = a.parse[Source]"
      val subTree = tree.find(q"parse").value

      val result = subTree.topStatement()

      result should equal(q"a.parse[Source]")
    }

    it("should include the whole pattern match") {
      val tree = q"variable match { case true => 1; case _ => 2 }"
      val defTree = q"def foo(variable: Boolean) = $tree"
      val subTree = defTree.find(q"true").value

      val result = subTree.topStatement()

      result should equal(q"variable match { case true => 1; case _ => 2 }")
    }

    it("should include the whole PartialFunction when matching on a def with a partialFunction") {
      val pf = q"{ case false => 1; case _ => 2 }"
      val defTree =
        q"""def foo: PartialFunction[Boolean, Int] = {
            val foo = bar
            $pf
          }"""
      val subTree = defTree.find(q"false").value

      val result = subTree.topStatement()

      result should equal(q"{ case false => 1; case _ => 2 }")
    }
  }

  describe("find") {
    // ignore until equality is fixed
    it("should find statement in simple tree") {
      val tree = q"val x = y >= 5"

      val result = tree.find(q">=")

      result.value should equal(q">=")
    }

    it("should find statement in large tree") {
      val tree =
        q"""def foo(list: List[Int], otherList: List[Int]) = {
        val firstResult = list
          .filter(_ % 2 == 0)
          .map(_ * 5)
          .reverse
        val secondResult = otherList
          .filter(_ >= 5)
          .map(_ * 3)
          .drop(5)
        (firstResult, secondResult)
      }"""

      val result = tree.find(q"_ * 5")

      result.value should equal(q"_ * 5")
    }

    it("should return none if statement is not in tree") {
      val tree = q"def four: Int = x < 5"

      val result = tree.find(q"x >= 5")

      result should be(None)
    }

    it("should still have parents when statement is found") {
      val original = q"x > 5"

      val result = original.find(q">").value

      result.parent.value should be theSameInstanceAs original
    }
  }

  describe("transformOnce") {

    /** If this test fails then that means the Scalameta transform works as we want it to
      * and our transformOnce can be replaced with it
      */
    it("normal transform causes a StackOverflowError") {
      val sut = q"def foo = 5"

      lazy val result = sut.transform({ case q"5" => q"5 + 1" })

      a[StackOverflowError] should be thrownBy result
    }

    it("should transform does not recursively transform new subtree") {
      val sut = q"def foo = 5"

      val result = sut.transformOnce({ case q"5" => q"5 + 1" })

      result should equal(q"def foo = 5 + 1")
    }

    it("should transform both appearances in the tree only once") {
      val sut = q"def foo = 5 + 5"

      val result = sut.transformOnce({ case q"5" => q"(5 * 2)" })

      result should equal(q"def foo = (5 * 2) + (5 * 2)")
    }

    it("should return the same tree if no transformation is applied") {
      val sut = q"def foo = 5"

      val result = sut.transformOnce({ case q"6" => q"6 + 1" })

      result should be theSameInstanceAs sut
    }

    it("should transform a parsed string and have changed syntax") {
      val sut = "val x: Int = 5".parse[Stat].get

      val result = sut.transformOnce({ case q"5" => q"6" })

      val expected = q"val x: Int = 6"
      result should equal(expected)
      result.syntax should equal(expected.syntax)
    }
  }
}
