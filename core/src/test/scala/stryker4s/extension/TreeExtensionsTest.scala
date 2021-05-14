package stryker4s.extension

import scala.meta._

import stryker4s.extension.TreeExtensions._
import stryker4s.testutil.Stryker4sSuite

class TreeExtensionsTest extends Stryker4sSuite {
  describe("topStatement") {
    it("should return top statement in a simple statement") {
      val tree = q"x.times(2)"
      val subTree = tree.find(q"times").value

      val result = subTree.topStatement()

      assert(subTree.isEqual(q"times"))
      result should be theSameInstanceAs tree
    }

    it("should be top of statement in def") {
      val tree = q"def foo(age: Int) = age.equals(18)"
      val subTree = tree.find(q"equals").value

      val result = subTree.topStatement()

      assert(subTree.isEqual(q"equals"))
      assert(result.isEqual(q"age.equals(18)"))
    }

    it("should return top statement on infix statement") {
      val tree = q"def foo(age: Int) = age equals 18"
      val subTree = tree.find(q"equals").value

      val result = subTree.topStatement()

      assert(subTree.isEqual(q"equals"))
      assert(result.isEqual(q"age equals 18"))
    }

    it("should return top statement on multiple calls") {
      val tree = q"def foo(list: List[Int]) = list.map(_ * 2).filter(_ >= 2).isEmpty"
      val subTree = tree.find(q">=").value

      val result = subTree.topStatement()

      assert(result.isEqual(q"list.map(_ * 2).filter(_ >= 2).isEmpty"))
    }

    it("should return top statement on list creation with method calls") {
      val tree = q"def foo() = List(1, 2, 3).filter(_ >= 2).isEmpty"
      val subTree = tree.find(q">=").value

      val result = subTree.topStatement()

      assert(result.isEqual(q"List(1, 2, 3).filter(_ >= 2).isEmpty"))
    }

    it("should return top statement on multiple calls when mutation is last call") {
      val tree = q"def foo(list: List[Int]) = list.map(_ * 2).filter(_ >= 2).isEmpty"
      val subTree = tree.find(q"isEmpty").value

      val result = subTree.topStatement()

      assert(result.isEqual(q"list.map(_ * 2).filter(_ >= 2).isEmpty"))
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
      assert(result.isEqual(expected))
    }

    it("should return same statement of def in def with single statement") {
      val tree = q"def four: Int = 4"
      val subTree = tree.find(q"4").value

      val result = subTree.topStatement()

      assert(result.isEqual(q"4"))
    }

    it("should return same statement when topStatement is called twice") {
      val tree = q"def four: Boolean = x >= 4"
      val subTree = tree.find(q">=").value

      val result = subTree
        .topStatement()
        .topStatement()

      assert(result.isEqual(q"x >= 4"))
    }

    it("should return whole statement with && and || operator") {
      val tree = q"def four(x: Int): Boolean = x >= 4 && x < 10 || x <= 0"
      val subTree = tree.find(q"<=").value

      val result = subTree.topStatement()

      assert(result.isEqual(q"x >= 4 && x < 10 || x <= 0"))
    }

    it("should return whole statement on infix inside postfix statement") {
      val tree = q"def foo = Math.square(2 * 5)"
      val subTree = tree.find(q"*").value

      val result = subTree.topStatement()

      assert(result.isEqual(q"Math.square(2 * 5)"))
    }

    it("should include the if statement") {
      val tree = q"def foo(x: Int) = { if(x > 5) x < 10 }"
      val subTree = tree.find(q"<").value

      val result = subTree.topStatement()

      assert(result.isEqual(q"x < 10"))
    }

    it("should include the if statement if the expression is in the condition-statement") {
      val tree = q"def foo(x: Int) = if(x >= 5) x > 10"
      val subTree = tree.find(q">=").value

      val result = subTree.topStatement()

      assert(result.isEqual(q"x >= 5"))
    }

    it("should include new operator") {
      val tree = q"def foo = new Bar(4).filter(_ >= 3)"
      val subTree = tree.find(q">=").value

      val result = subTree.topStatement()

      assert(result.isEqual(q"new Bar(4).filter(_ >= 3)"))
    }

    it("should include entire statement with && statement") {
      val tree = q"def foo = a == b && b == c"
      val subTree = tree.find(q"&&").value

      val result = subTree.topStatement()

      assert(result.isEqual(q"a == b && b == c"))
    }

    it("should include entire statement when && is not symmetrical on left") {
      val tree = q"def foo = a && b == c"
      val subTree = tree.find(q"&&").value

      val result = subTree.topStatement()

      assert(result.isEqual(q"a && b == c"))
    }

    it("should include entire statement when && is not symmetrical on right") {
      val tree = q"def foo = a == b && c"
      val subTree = tree.find(q"&&").value

      val result = subTree.topStatement()

      assert(result.isEqual(q"a == b && c"))
    }

    it("should include generic type") {
      val tree = q"def foo = a.parse[Source]"
      val subTree = tree.find(q"parse").value

      val result = subTree.topStatement()

      assert(result.isEqual(q"a.parse[Source]"))
    }

    it("should include the whole pattern match") {
      val tree = q"variable match { case true => 1; case _ => 2 }"
      val defTree = q"def foo(variable: Boolean) = $tree"
      val subTree = defTree.find(q"true").value

      val result = subTree.topStatement()

      assert(result.isEqual(q"variable match { case true => 1; case _ => 2 }"))
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

      assert(result.isEqual(q"{ case false => 1; case _ => 2 }"))
    }

    it("should match on more than the single variable in a Pat.Alternative") {
      val tree = q"variable match { case 1 | 2 => 3; case _ => 4 }"
      val defTree = q"def foo(variable: Int) = $tree"
      val subTree = defTree.find(q"2").value

      val result = subTree.topStatement()

      assert(result.isEqual(q"variable match { case 1 | 2 => 3; case _ => 4 }"))
    }

    it("should match on more than the single variable in a Pat.Extract") {
      val hello = Lit.String("hello")
      val tree = q"variable match { case GET -> Root / $hello => 3; case _ => 4 }"
      val defTree = q"def foo(variable: Int) = $tree"
      val subTree = defTree.find(hello).value

      val result = subTree.topStatement()

      assert(result.isEqual(q"variable match { case GET -> Root / $hello => 3; case _ => 4 }"))
    }

    it("should stop before the body of a Case") {
      val pf = q"{ case false => { foo(1); 2 }; case _ => 3 }"
      val defTree =
        q"""def foo: PartialFunction[Boolean, Int] = {
            val foo = bar
            $pf
          }"""
      val subTree = defTree.find(q"1").value

      val result = subTree.topStatement()

      assert(result.isEqual(q"foo(1)"))
    }

    it("should match on a Literal") {
      val tree = q"def foo = list.map(_ == 4)"
      val subTree = tree.find(q"4").value

      val result = subTree.topStatement()

      assert(result.isEqual(q"list.map(_ == 4)"))
    }

    it("should include ! in if statement") {
      val tree = q"if(!foo) bar else baz"
      val subTree = tree.find(q"foo").value

      val result = subTree.topStatement()

      assert(result.isEqual(q"!foo"))
    }

    it("should include pattern matches") {
      val expectedTopStatement = q"""list.nonEmpty match {
        case true => someValue
        case _ => otherValue
      }"""
      val tree = q"""def foo = { otherValue; $expectedTopStatement }"""
      val subTree = tree.find(q"nonEmpty").value

      val result = subTree.topStatement()

      assert(result.isEqual(expectedTopStatement), result)
    }

    it("should not include a class as a topStatement") {
      val tree = source"""class Foo { 
        myFunction()
      }"""
      val subTree = tree.find(q"myFunction()").value

      val result = subTree.topStatement()

      assert(result.isEqual(q"myFunction()"))
    }

    it("should run on a for-comprehension") {
      val tree = q"""def foo = for {
        username <- readEnvironmentVariable("baz", env)
        repoName <- foo.bar
      } yield username + repoName"""
      val subTree = tree.find(q"bar").value

      val result = subTree.topStatement()

      assert(result.isEqual(q"foo.bar"))
    }

    it("should run on a for-comprehension yield") {
      val tree = q"""def foo = for {
                      baz <- foo.bar
                    } yield baz.qux"""
      val subTree = tree.find(q"baz.qux").value

      val result = subTree.topStatement()

      assert(result.isEqual(q"baz.qux"))
    }

    it("should stop at the catch in a try-catch") {
      val tree = q"""def foo = try {
                      foo.bar
                    } catch {
                      case _ => baz.qux
                    }"""
      val subTree = tree.find(q"baz.qux").value

      val result = subTree.topStatement()

      assert(result.isEqual(q"baz.qux"))
    }

    it("should stop at named argument assignments") {
      val tree = q"def foo = bar(baz = true)"
      val subTree = tree.find(q"true").value

      val result = subTree.topStatement()

      assert(result.isEqual(q"true"), result)
    }

    it("should stop at named argument assignments for inheritance assignment") {
      val tree = q"case object GZIP extends Header(juzDeflaterNoWrap = true)"
      val subTree = tree.find(q"true").value

      val result = subTree.topStatement()

      assert(result.isEqual(q"true"))
    }
  }

  describe("find") {
    // ignore until equality is fixed
    it("should find statement in simple tree") {
      val tree = q"val x = y >= 5"

      val result = tree.find(q">=")

      assert(result.value.isEqual(q">="))
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

      assert(result.value.isEqual(q"_ * 5"))
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

    it("should transform does not recursively transform new subtree") {
      val sut = q"def foo = 5"

      val result = sut.transformOnce({ case q"5" => q"5 + 1" }).get

      assert(result.isEqual(q"def foo = 5 + 1"))
    }

    it("should transform both appearances in the tree only once") {
      val sut = q"def foo = 5 + 5"

      val result = sut.transformOnce({ case q"5" => q"(5 * 2)" }).get

      assert(result.isEqual(q"def foo = (5 * 2) + (5 * 2)"))
    }

    it("should return the same tree if no transformation is applied") {
      val sut = q"def foo = 5"

      val result = sut.transformOnce({ case q"6" => q"6 + 1" }).get

      result should be theSameInstanceAs sut
    }

    it("should transform a parsed string and have changed syntax") {
      val sut = "val x: Int = 5".parse[Stat].get

      val result = sut.transformOnce({ case q"5" => q"6" }).get

      val expected = q"val x: Int = 6"
      assert(result.isEqual(expected))
      result.syntax should equal(expected.syntax)
    }
  }
}
