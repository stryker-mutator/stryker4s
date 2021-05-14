package stryker4s.mutants.findmutants

import scala.meta._

import stryker4s.config.Config
import stryker4s.extension.ImplicitMutationConversion.mutationToTree
import stryker4s.extension.TreeExtensions.IsEqualExtension
import stryker4s.extension.mutationtype._
import stryker4s.model.Mutant
import stryker4s.testutil.Stryker4sSuite
import stryker4s.model.IgnoredMutationReason

class MutantMatcherTest extends Stryker4sSuite {
  implicit private val config: Config = Config.default
  private val sut = new MutantMatcher()

  def expectMutations(
      matchFun: PartialFunction[Tree, Seq[Either[IgnoredMutationReason, Mutant]]],
      tree: Tree,
      original: Term,
      expectedTerms: Term*
  )(implicit expectedName: String): Unit = {
    val found = tree.collect(matchFun).flatten

    expectedTerms.foreach(expectedTerm => expectMutations(found, original, expectedTerm))
  }

  def expectMutations(
      actualMutants: Seq[Either[IgnoredMutationReason, Mutant]],
      original: Term,
      expectedMutations: Term*
  )(implicit
      expectedName: String
  ): Unit = {
    expectedMutations.foreach(expectedMutation => {
      val actualMutant = actualMutants
        .collect { case Right(v) => v }
        .find(mutant =>
          mutant.mutated.isEqual(expectedMutation) &&
            mutant.original.isEqual(original)
        )
        .getOrElse(fail("mutant not found"))

      assert(actualMutant.original.isEqual(original))
      assert(actualMutant.mutated.isEqual(expectedMutation))
      actualMutant.mutationType.mutationName shouldBe expectedName
    })
  }

  /** Check if there is a mutant for every expected mutation
    */
  def expectedMutations(
      matchFun: PartialFunction[Tree, Seq[Either[IgnoredMutationReason, Mutant]]],
      tree: Tree,
      expectedMutations: MethodExpression*
  ): Unit = {
    val found = tree.collect(matchFun).flatten.collect { case Right(v) => v }
    expectedMutations foreach { expectedMutation =>
      found
        .map(_.mutated)
        .collectFirst { case expectedMutation(_, _) => }
        .getOrElse(fail("mutant not found"))
    }
  }

  describe("All Matchers") {
    it("should match a conditional statement") {
      val tree = q"def foo = 15 > 20 && 20 < 15"

      val found = tree.collect(sut.allMatchers).flatten

      found should have length 7
      expectMutations(found, q">", q">=", q"<", q"==")("EqualityOperator")
      expectMutations(found, q"&&", q"||")("LogicalOperator")
      expectMutations(found, q"<", q"<=", q">", q"==")("EqualityOperator")
    }

    it("should match a method") {
      val tree = q"def foo = List(1, 2).filterNot(filterNotFunc).filter(filterFunc)"

      val found = tree.collect(sut.allMatchers).flatten

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

      val found = tree.collect(sut.allMatchers).flatten

      found should have length 5
      expectMutations(found, q"false", q"true")("BooleanLiteral")
      expectMutations(found, q"&&", q"||")("LogicalOperator")
      expectMutations(found, q">", q"<", q"==")("EqualityOperator")
    }

    it("should match the default case of a constructor argument") {
      val tree = q"class Person(isOld: Boolean = 18 > 15) { }"

      val found = tree.collect(sut.allMatchers).flatten

      found should have length 3
      expectMutations(found, q">", q">=", q"<", q"==")("EqualityOperator")
    }

    it("should match on the default case of a function argument") {
      val tree = q"def hasGoodBack(isOld: Boolean = age > 60): Boolean = isOld"

      val found = tree.collect(sut.allMatchers).flatten

      found should have length 3
      expectMutations(found, q">", q">=", q"<", q"==")("EqualityOperator")
    }
  }

  describe("matchEqualityOperator matcher") {
    implicit val mutatorName = "EqualityOperator"
    it("should match >= sign with >, <, and ==") {
      expectMutations(
        sut.matchEqualityOperator,
        q"def foo = 18 >= 20",
        GreaterThanEqualTo,
        GreaterThan,
        LesserThan,
        EqualTo
      )
    }

    it("should match > with >=, < and ==") {
      expectMutations(
        sut.matchEqualityOperator,
        q"def foo = 18 > 20",
        GreaterThan,
        GreaterThanEqualTo,
        LesserThan,
        EqualTo
      )
    }

    it("should match <= to <, >= and ==") {
      expectMutations(
        sut.matchEqualityOperator,
        q"def foo = 18 <= 20",
        LesserThanEqualTo,
        LesserThan,
        GreaterThanEqualTo,
        EqualTo
      )
    }

    it("should match < to <=, > and ==") {
      expectMutations(
        sut.matchEqualityOperator,
        q"def foo = 18 < 20",
        LesserThan,
        LesserThanEqualTo,
        GreaterThan,
        EqualTo
      )
    }

    it("should match == to !=") {
      expectMutations(
        sut.matchEqualityOperator,
        q"def foo = 18 == 20",
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
      val tree = source"""class Foo {
        def bar = {
          case "str" => 4
        }
      }"""
      expectMutations(
        sut.matchStringLiteral,
        tree,
        str,
        Lit.String("")
      )
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
        RegularExpression(".")
      )
    }

    it("should match scala.util.matching.Regex constructor") {
      expectMutations(
        sut.matchRegex,
        q"""def foo = new scala.util.matching.Regex($regex)""",
        regex,
        RegularExpression(".")
      )
    }

    it("should match a Regex constructor with named groups") {
      expectMutations(
        sut.matchRegex,
        q"""def foo = new Regex($regex, "any")""",
        regex,
        RegularExpression(".")
      )
    }

    it("should match Regex String ops") {
      expectMutations(
        sut.matchRegex,
        q"""def foo = $regex.r""",
        regex,
        RegularExpression(".")
      )
    }

    it("should match Pattern.compile Regex constructor") {
      expectMutations(
        sut.matchRegex,
        q"""def foo = Pattern.compile($regex)""",
        regex,
        RegularExpression(".")
      )
    }

    it("should match java.util.regex.Pattern.compile Regex constructor") {
      expectMutations(
        sut.matchRegex,
        q"""def foo = java.util.regex.Pattern.compile($regex)""",
        regex,
        RegularExpression(".")
      )
    }

    it("should match Pattern.compile Regex constructor with flags") {
      expectMutations(
        sut.matchRegex,
        q"""def foo = Pattern.compile($regex, CASE_INSENSITIVE)""",
        regex,
        RegularExpression(".")
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

  describe("Create mutant id's") {
    it("should register multiple mutants from a found mutant with multiple mutations") {
      val sut = new MutantMatcher()
      val mutants =
        sut.TermExtensions(GreaterThan).~~>(LesserThan, GreaterThanEqualTo, EqualTo).collect { case Right(v) => v }

      mutants.map(_.id) should contain theSameElementsAs List(0, 1, 2)
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
      val tree = q"type Foo = String Refined StartsWith[${Lit.String("jdbc:")}]"

      val result = tree collect sut.allMatchers

      result.flatten should be(empty)
    }

    it("should not match on infix type arguments") {
      val tree = q"type Foo = String Refined ${Lit.String("jdbc:")}"

      val result = tree collect sut.allMatchers

      result.flatten should be(empty)
    }

    it("should match a function with a single expression") {
      val tree = q"def isEmpty = exists"

      val result = tree.collect(sut.allMatchers).flatten.collect { case Right(v) => v }

      result.map(_.original) should not contain q"isEmpty"
    }

    it("should mutate if statements with true and false as condition") {
      val tree = q"if(aVariable) { println }"

      val found = tree.collect(sut.allMatchers).flatten

      found should have length 2
      expectMutations(found, q"aVariable", q"true")("ConditionalExpression")
      expectMutations(found, q"aVariable", q"false")("ConditionalExpression")
    }

    it("should mutate while statements with false as condition") {
      val tree = q"while(aVariable) { println }"

      val found = tree.collect(sut.allMatchers).flatten

      found should have length 1
      expectMutations(found, q"aVariable", q"false")("ConditionalExpression")
    }

    it("should mutate do while statements with false as condition") {
      val tree = q"do { println } while(aVariable)"

      val found = tree.collect(sut.allMatchers).flatten

      found should have length 1
      expectMutations(found, q"aVariable", q"false")("ConditionalExpression")
    }

    it("should mutate conditional statements that have a literal boolean as condition only once") {
      val trueTree = q"if(true) { println }"
      val falseTree = q"if(false) { println }"

      val trueFound = trueTree.collect(sut.allMatchers).flatten
      val falseFound = falseTree.collect(sut.allMatchers).flatten

      trueFound should have length 1
      falseFound should have length 1
      expectMutations(trueFound, q"true", q"false")("BooleanLiteral")
      expectMutations(falseFound, q"false", q"true")("BooleanLiteral")
    }
  }
}
