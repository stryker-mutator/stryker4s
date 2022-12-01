package stryker4jvm.mutator.scala.mutants.findmutants

import stryker4jvm.config.Config
import stryker4jvm.mutants.findmutants.MutantMatcher.MutationMatcher
import stryker4jvm.mutants.tree.{IgnoredMutations, Mutations}
import stryker4jvm.mutator.scala.testutil.Stryker4sSuite

import scala.meta.*

class MutantMatcherTest extends Stryker4sSuite {
  implicit private val config: Config = Config.default
  private val sut = new MutantMatcherImpl()

  def expectMutations(
      matchFun: MutationMatcher,
      tree: Defn.Def,
      original: Term,
      expectedTerms: Term*
  )(implicit expectedName: String): Unit = {
    val found = tree.collect(matchFun).map(_(PlaceableTree(tree.body)))

    expectedTerms.foreach(expectedTerm => expectMutations(found, original, expectedTerm))
  }

  def expectMutations(
      actualMutants: List[Either[IgnoredMutations, Mutations]],
      original: Term,
      expectedMutations: Term*
  )(implicit
      expectedName: String
  ): Unit = {

    expectedMutations.foreach { expectedMutation =>
      val actualMutant = actualMutants
        .flatMap(_.toSeq)
        .flatMap(_.toVector)
        .map(_.metaData)
        .find(m => m.original == original.syntax && m.replacement == expectedMutation.syntax)
        .getOrElse(fail(s"mutant $expectedMutation not found"))

      actualMutant.original shouldBe original.syntax
      actualMutant.replacement shouldBe expectedMutation.syntax
      actualMutant.mutatorName shouldBe expectedName
    }
  }

  /** Check if there is a mutant for every expected mutation
    */
  def expectedMutations(
      matchFun: MutationMatcher,
      tree: Defn.Def,
      expectedMutations: MethodExpression*
  ): Unit = {
    val found =
      tree.collect(matchFun).map(_(PlaceableTree(tree.body))).flatMap(_.toSeq).flatMap(_.toVector)
    expectedMutations foreach { expectedMutation =>
      found
        .map(_.mutatedStatement)
        .collectFirst { case expectedMutation(_, _) => }
        .getOrElse(fail("mutant not found"))
    }
  }

  describe("All Matchers") {
    it("should match a conditional statement") {
      val tree = q"def foo = 15 > 20 && 20 < 15"
      val found = tree.collect(sut.allMatchers).map(_(PlaceableTree(tree.body)))

      found.flatMap(_.toSeq).flatMap(_.toVector) should have length 7
      expectMutations(found, q">", q">=", q"<", q"==")("EqualityOperator")
      expectMutations(found, q"&&", q"||")("LogicalOperator")
      expectMutations(found, q"<", q"<=", q">", q"==")("EqualityOperator")
    }

    it("should match a method") {
      val tree = q"def foo = List(1, 2).filterNot(filterNotFunc).filter(filterFunc)"

      val found = tree.collect(sut.allMatchers).map(_(PlaceableTree(tree.body)))

      found should have length 2
      expectMutations(found, q"List(1, 2).filterNot(filterNotFunc)", q"List(1, 2).filter(filterNotFunc)")(
        "MethodExpression"
      )
      expectMutations(
        found,
        q"List(1, 2).filterNot(filterNotFunc).filter(filterFunc)",
        q"List(1, 2).filterNot(filterNotFunc).filterNot(filterFunc)"
      )("MethodExpression")
    }

    it("should match a boolean and a conditional") {
      val tree = q"def foo = false && 15 > 4"

      val found = tree.collect(sut.allMatchers).map(_(PlaceableTree(tree.body)))

      found.flatMap(_.toSeq).flatMap(_.toVector) should have length 5
      expectMutations(found, q"false", q"true")("BooleanLiteral")
      expectMutations(found, q"&&", q"||")("LogicalOperator")
      expectMutations(found, q">", q"<", q"==")("EqualityOperator")
    }

    it("should match the default case of a constructor argument") {
      val tree = q"class Person(isOld: Boolean = 18 > 15) { }"

      val found = tree.collect(sut.allMatchers).map(_(PlaceableTree(tree.find(q"18 > 15").value)))

      found.flatMap(_.toSeq).flatMap(_.toVector) should have length 3
      expectMutations(found, q">", q">=", q"<", q"==")("EqualityOperator")
    }

    it("should match on the default case of a function argument") {
      val tree = q"def hasGoodBack(isOld: Boolean = age > 60): Boolean = isOld"

      val found = tree.collect(sut.allMatchers).map(_(PlaceableTree(tree.find(q"age > 60").value)))

      found.flatMap(_.toSeq).flatMap(_.toVector) should have length 3
      expectMutations(found, q">", q">=", q"<", q"==")("EqualityOperator")
    }
  }

  describe("matchEqualityOperator matcher") {
    implicit val mutatorName = "EqualityOperator"
    it("should match >= sign with >, <, and ==") {
      val tree = q"def foo = 18 >= 20"
      val found = tree.collect(sut.matchEqualityOperator).map(_(PlaceableTree(tree.body)))
      expectMutations(
        found,
        GreaterThanEqualTo,
        GreaterThan,
        LesserThan,
        EqualTo
      )
    }

    it("should match > with >=, < and ==") {
      val tree = q"def foo = 18 > 20"
      val found = tree.collect(sut.matchEqualityOperator).map(_(PlaceableTree(tree.body)))
      expectMutations(
        found,
        GreaterThan,
        GreaterThanEqualTo,
        LesserThan,
        EqualTo
      )
    }

    it("should match <= to <, >= and ==") {
      val tree = q"def foo = 18 <= 20"
      val found = tree.collect(sut.matchEqualityOperator).map(_(PlaceableTree(tree.body)))
      expectMutations(
        found,
        LesserThanEqualTo,
        LesserThan,
        GreaterThanEqualTo,
        EqualTo
      )
    }

    it("should match < to <=, > and ==") {
      val tree = q"def foo = 18 < 20"
      val found = tree.collect(sut.matchEqualityOperator).map(_(PlaceableTree(tree.body)))
      expectMutations(
        found,
        LesserThan,
        LesserThanEqualTo,
        GreaterThan,
        EqualTo
      )
    }

    it("should match == to !=") {
      val tree = q"def foo = 18 == 20"
      val found = tree.collect(sut.matchEqualityOperator).map(_(PlaceableTree(tree.body)))
      expectMutations(
        found,
        EqualTo,
        NotEqualTo
      )
    }

    it("should match != to ==") {
      expectMutations(
        sut.matchEqualityOperator,
        q"def foo = 18 != 20",
        NotEqualTo,
        EqualTo
      )
    }

    it("should match === to =!=") {
      expectMutations(
        sut.matchEqualityOperator,
        q"def foo = 18 === 20",
        TypedEqualTo,
        TypedNotEqualTo
      )
    }

    it("should match =!= to ===") {
      expectMutations(
        sut.matchEqualityOperator,
        q"def foo = 18 =!= 20",
        TypedNotEqualTo,
        TypedEqualTo
      )
    }
  }
  describe("matchLogicalOperator matcher") {
    implicit val mutatorName = "LogicalOperator"

    it("should match && to ||") {
      expectMutations(
        sut.matchLogicalOperator,
        q"def foo = a && b",
        And,
        Or
      )
    }

    it("should match || to &&") {
      expectMutations(
        sut.matchLogicalOperator,
        q"def foo = a || b",
        Or,
        And
      )
    }
  }

  describe("matchMethodExpression matcher") {
    it("should match filter to filterNot") {
      expectedMutations(sut.matchMethodExpression, q"def foo = List(1, 2, 3).filter(_ % 2 == 0)", FilterNot)
    }

    it("should match filterNot to filter") {
      expectedMutations(sut.matchMethodExpression, q"def foo = List(1, 2, 3).filterNot(_ % 2 == 0)", Filter)
    }

    it("should match exists to forall") {
      expectedMutations(sut.matchMethodExpression, q"def foo = List(1, 2, 3).exists(_ % 2 == 0)", Forall)
    }

    it("should match forall to exists") {
      expectedMutations(sut.matchMethodExpression, q"def foo = List(1, 2, 3).forall(_ % 2 == 0)", Exists)
    }

    it("should match take to drop") {
      expectedMutations(sut.matchMethodExpression, q"def foo = List(1, 2, 3).take(2)", Drop)
    }

    it("should match drop to take") {
      expectedMutations(sut.matchMethodExpression, q"def foo = List(1, 2, 3).drop(2)", Take)
    }

    it("should match takeRight to dropRight") {
      expectedMutations(sut.matchMethodExpression, q"def foo = List(1, 2, 3).takeRight(2)", DropRight)
    }

    it("should match dropRight to takeRight") {
      expectedMutations(sut.matchMethodExpression, q"def foo = List(1, 2, 3).dropRight(2)", TakeRight)
    }

    it("should match takeWhile to dropWhile") {
      expectedMutations(sut.matchMethodExpression, q"def foo = List(1, 2, 3).dropWhile(_ < 2)", TakeWhile)
    }

    it("should match dropWhile to takeWhile") {
      expectedMutations(sut.matchMethodExpression, q"def foo = List(1, 2, 3).takeWhile(_ < 2)", DropWhile)
    }

    it("should match isEmpty to nonEmpty") {
      expectedMutations(sut.matchMethodExpression, q"def foo = List(1, 2, 3).isEmpty", NonEmpty)
    }

    it("should match nonEmpty to isEmpty") {
      expectedMutations(sut.matchMethodExpression, q"def foo = List(1, 2, 3).nonEmpty", IsEmpty)
    }

    it("should match indexOf to lastIndexOf") {
      expectedMutations(sut.matchMethodExpression, q"def foo = List(1, 2, 3).indexOf(2)", LastIndexOf)
    }

    it("should match lastIndexOf to indexOf") {
      expectedMutations(sut.matchMethodExpression, q"def foo = List(1, 2, 3).lastIndexOf(2)", IndexOf)
    }

    it("should match max to min") {
      expectedMutations(sut.matchMethodExpression, q"def foo = List(1, 2, 3).max", Min)
    }

    it("should match min to max") {
      expectedMutations(sut.matchMethodExpression, q"def foo = List(1, 2, 3).min", Max)
    }

    it("should match maxBy to minBy") {
      expectedMutations(sut.matchMethodExpression, q"def foo = List(1, 2, 3).maxBy(_.toString)", MinBy)
    }

    it("should match minBy to maxBy") {
      expectedMutations(sut.matchMethodExpression, q"def foo = List(1, 2, 3).minBy(_.toString)", MaxBy)
    }
  }

  describe("matchBooleanLiteral matcher") {
    implicit val mutatorName = "BooleanLiteral"

    it("should match false to true") {
      expectMutations(
        sut.matchBooleanLiteral,
        q"def foo = false",
        False,
        True
      )
    }

    it("should match true to false") {
      expectMutations(
        sut.matchBooleanLiteral,
        q"def foo = true",
        True,
        False
      )
    }
  }
  describe("matchStringLiteral matcher") {
    implicit val mutatorName = "StringLiteral"

    it("should match foo to NonEmptyString") {
      expectMutations(
        sut.matchStringLiteral,
        q"""def foo: String = "bar"""",
        Lit.String("bar"),
        EmptyString
      )
    }

    it("should match empty string to StrykerWasHere") {
      expectMutations(
        sut.matchStringLiteral,
        q"""def foo = "" """,
        EmptyString,
        StrykerWasHereString
      )
    }

    it("should match on interpolated strings") {
      val interpolated =
        Term.Interpolate(q"s", List(Lit.String("interpolate "), Lit.String("")), List(q"foo"))
      val tree = q"def foo = $interpolated"
      val emptyString = Lit.String("")

      interpolated.syntax should equal("s\"interpolate $foo\"")
      expectMutations(
        sut.matchStringLiteral,
        tree,
        interpolated,
        emptyString
      )
    }

    it("should match once on interpolated strings with multiple parts") {
      val interpolated =
        Term.Interpolate(
          q"s",
          List(Lit.String("interpolate "), Lit.String(" foo "), Lit.String(" bar")),
          List(q"fooVar", q"barVar + 1")
        )
      val tree = q"def foo = $interpolated"
      val emptyString = Lit.String("")

      interpolated.syntax should equal("s\"interpolate $fooVar foo ${barVar" + " + 1} bar\"")
      expectMutations(
        sut.matchStringLiteral,
        tree,
        interpolated,
        emptyString
      )
    }

    it("should not match non-string interpolation") {
      val interpolated =
        Term.Interpolate(q"q", List(Lit.String("interpolate "), Lit.String("")), List(q"foo"))
      val tree = q"def foo = $interpolated "

      val result = tree collect sut.allMatchers

      interpolated.syntax should equal("q\"interpolate $foo\"")
      result should be(empty)
    }

    it("should not match pattern interpolation") {
      val tree = source"""class Foo {
        def bar = {
          case t"interpolate" => _
        }
      }"""

      val result = tree collect sut.allMatchers

      result should be(empty)
    }

    it("should match pattern in string") {
      val str = Lit.String("str")
      val tree = q"""def bar = {
          case "str" => 4
        }"""
      expectMutations(
        sut.matchStringLiteral,
        tree,
        str,
        Lit.String("")
      )
    }

    it("should not match xml literals") {
      val tree = source"""class Foo {
        def bar = ${Term.Xml(List(Lit.String("<foo>"), Lit.String("</foo>")), List(q"foo"))}
      }"""

      val result = tree collect sut.allMatchers

      result should be(empty)
    }

    it("should not match empty strings on xml literals") {
      val tree = source"""class Foo {
        def bar = ${Term.Xml(List(Lit.String("<foo>"), Lit.String("")), List(q"foo"))}
      }"""

      val result = tree collect sut.allMatchers

      result should be(empty)
    }

    it("should match inside xml literal args") {
      val str = Lit.String("str")
      val tree = q"""def bar = ${Term.Xml(List(Lit.String("<foo>"), Lit.String("</foo>")), List(str))}"""
      expectMutations(
        sut.matchStringLiteral,
        tree,
        str,
        Lit.String("")
      )
    }

    it("should not match xml interpolation") {
      val tree = Pat.Xml(List(Lit.String("<foo></xml>")), List.empty)

      val result = tree collect sut.allMatchers

      result should be(empty)
    }
  }

  describe("regexMutator") {
    implicit val mutatorName = "RegularExpression"
    val regex = Lit.String(".*")

    it("should match Regex constructor") {
      val tree = q"""def foo = new Regex($regex)"""

      expectMutations(
        sut.matchRegex,
        tree,
        regex,
        RegularExpression(".", regex.pos.toLocation)
      )
    }

    it("should match scala.util.matching.Regex constructor") {
      expectMutations(
        sut.matchRegex,
        q"""def foo = new scala.util.matching.Regex($regex)""",
        regex,
        RegularExpression(".", regex.pos.toLocation)
      )
    }

    it("should match a Regex constructor with named groups") {
      expectMutations(
        sut.matchRegex,
        q"""def foo = new Regex($regex, "any")""",
        regex,
        RegularExpression(".", regex.pos.toLocation)
      )
    }

    it("should match Regex String ops") {
      expectMutations(
        sut.matchRegex,
        q"""def foo = $regex.r""",
        regex,
        RegularExpression(".", regex.pos.toLocation)
      )
    }

    it("should match Pattern.compile Regex constructor") {
      expectMutations(
        sut.matchRegex,
        q"""def foo = Pattern.compile($regex)""",
        regex,
        RegularExpression(".", regex.pos.toLocation)
      )
    }

    it("should match java.util.regex.Pattern.compile Regex constructor") {
      expectMutations(
        sut.matchRegex,
        q"""def foo = java.util.regex.Pattern.compile($regex)""",
        regex,
        RegularExpression(".", regex.pos.toLocation)
      )
    }

    it("should match Pattern.compile Regex constructor with flags") {
      expectMutations(
        sut.matchRegex,
        q"""def foo = Pattern.compile($regex, CASE_INSENSITIVE)""",
        regex,
        RegularExpression(".", regex.pos.toLocation)
      )
    }

    it("should not match e regular string") {
      expectMutations(
        sut.matchRegex,
        q"""def foo = $regex""",
        regex
      )
    }

  }

  describe("no function name matching") {
    it("should not match a function with a mutator name") {
      val tree = q"def isEmpty = foo"

      val result = tree collect sut.allMatchers

      result should be(empty)
    }

    it("should not match on a case class with a mutator name") {
      val tree = q"case class indexOf(foo: String)"

      val result = tree collect sut.allMatchers

      result should be(empty)
    }

    it("should not match on a variable with a mutator name") {
      val tree = q"val min = 5"

      val result = tree collect sut.allMatchers

      result should be(empty)
    }

    it("should not match on type arguments") {
      val tree = t"String Refined StartsWith[${Lit.String("jdbc:")}]"

      val result = tree collect sut.allMatchers

      result should be(empty)
    }

    it("should not match on infix type arguments") {
      val tree = t"String Refined ${Lit.String("jdbc:")}"

      val result = tree collect sut.allMatchers

      result should be(empty)
    }

    it("should not match on type apply") {
      val tree = q"foo[${Lit.String("jdbc:")}]()"

      val result = tree collect sut.allMatchers

      result should be(empty)
    }

    it("should not match on literal type declarations") {
      val tree = q"val a: ${Lit.String("4")} = ???"

      val result = tree collect sut.allMatchers

      result should be(empty)
    }

    it("should not match on literal type declarations for var") {
      val tree = q"var a: ${Lit.String("4")} = ???"

      val result = tree collect sut.allMatchers

      result should be(empty)
    }

    it("should not match on infix literal type declarations") {
      val tree = q"val a: ${Lit.String("4")} + ${Lit.String("6")} = ???"

      val result = tree collect sut.allMatchers

      result should be(empty)
    }

    it("should not match on def literal return types") {
      val tree = q"def a: ${Lit.String("4")} = ???"

      val result = tree collect sut.allMatchers

      result should be(empty)
    }

    it("should not match on literal function types") {
      val tree = q"def a: (Int => ${Lit.String("4")}) = ???"

      val result = tree collect sut.allMatchers

      result should be(empty)
    }

    it("should not match on type aliases") {
      val tree = q"type Foo = ${Lit.String("4")}"

      val result = tree collect sut.allMatchers

      result should be(empty)
    }

    it("should not match a function with a single expression") {
      val tree = q"def isEmpty = exists"

      val result = tree
        .collect(sut.allMatchers)
        .map(_(PlaceableTree(tree.body)))
        .flatMap(_.toSeq)
        .flatMap(_.toVector)
        .map(_.metaData)

      result.map(_.original) should not contain "isEmpty"
    }

    it("should mutate if statements with true and false as condition") {
      val tree = q"if(aVariable) { println }"

      val found = tree.collect(sut.allMatchers).map(_(PlaceableTree(tree)))

      found.flatMap(_.toSeq).flatMap(_.toVector) should have length 2
      expectMutations(found, q"aVariable", q"true")("ConditionalExpression")
      expectMutations(found, q"aVariable", q"false")("ConditionalExpression")
    }

    it("should mutate while statements with false as condition") {
      val tree = q"while(aVariable) { println }"

      val found = tree.collect(sut.allMatchers).map(_(PlaceableTree(tree)))

      found.flatMap(_.toSeq).flatMap(_.toVector) should have length 1
      expectMutations(found, q"aVariable", q"false")("ConditionalExpression")
    }

    it("should mutate do while statements with false as condition") {
      val tree = q"do { println } while(aVariable)"

      val found = tree.collect(sut.allMatchers).map(_(PlaceableTree(tree)))

      found.flatMap(_.toSeq).flatMap(_.toVector) should have length 1
      expectMutations(found, q"aVariable", q"false")("ConditionalExpression")
    }

    it("should mutate conditional statements that have a literal boolean as condition only once") {
      val trueTree = q"if(true) { println }"
      val falseTree = q"if(false) { println }"

      val trueFound = trueTree.collect(sut.allMatchers).map(_(PlaceableTree(trueTree)))
      val falseFound = falseTree.collect(sut.allMatchers).map(_(PlaceableTree(falseTree)))

      trueFound should have length 1
      falseFound should have length 1
      expectMutations(trueFound, q"true", q"false")("BooleanLiteral")
      expectMutations(falseFound, q"false", q"true")("BooleanLiteral")
    }
  }
}
