package stryker4s.mutants.findmutants

import munit.Location
import stryker4s.config.Config
import stryker4s.extension.TreeExtensions.FindExtension
import stryker4s.model.{MutationExcluded, NoRegexMutationsFound, PlaceableTree, RegexParseError}
import stryker4s.mutants.findmutants.MutantMatcher.MutationMatcher
import stryker4s.mutants.tree.{IgnoredMutations, Mutations}
import stryker4s.mutation.*
import stryker4s.testkit.Stryker4sSuite

import scala.meta.*

class MutantMatcherTest extends Stryker4sSuite {
  implicit private val config: Config = Config.default
  private val sut = new MutantMatcherImpl()

  def expectMutations(
      matchFun: MutationMatcher,
      tree: Defn.Def,
      original: Term,
      expectedTerms: Term*
  )(implicit expectedName: String, loc: Location): Unit = {
    val found = tree.collect(matchFun).map(_(PlaceableTree(tree.body)))

    assertEquals(found.flatMap(_.toSeq).length, expectedTerms.length)
    expectedTerms.foreach(expectedTerm => expectMutations(found, original, expectedTerm))
  }

  def expectMutations(
      actualMutants: List[Either[IgnoredMutations, Mutations]],
      original: Term,
      expectedMutations: Term*
  )(implicit
      expectedName: String,
      loc: Location
  ): Unit = {

    expectedMutations.foreach { expectedMutation =>
      val actualMutant = actualMutants
        .flatMap(_.toSeq)
        .flatMap(_.toVector)
        .map(_.metadata)
        .find(m => m.original == original.syntax && expectedMutation.syntax.contains(m.replacement))
        .getOrElse(fail(s"mutant $expectedMutation not found"))

      assertEquals(actualMutant.mutatorName, expectedName)
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
    test("should match a conditional statement") {
      val tree = q"def foo = 15 > 20 && 20 < 15"
      val found = tree.collect(sut.allMatchers).map(_(PlaceableTree(tree.body)))

      assertEquals(found.flatMap(_.toSeq).flatMap(_.toVector).length, 7)
      expectMutations(found, q">", q">=", q"<", q"==")("EqualityOperator", implicitly)
      expectMutations(found, q"&&", q"||")("LogicalOperator", implicitly)
      expectMutations(found, q"<", q"<=", q">", q"==")("EqualityOperator", implicitly)
    }

    test("should match a method") {
      implicit val mutatorName = "MethodExpression"
      val tree = q"def foo = List(1, 2).filterNot(filterNotFunc).filter(filterFunc)"
      val found = tree.collect(sut.allMatchers).map(_(PlaceableTree(tree.body)))

      assertEquals(found.length, 2)
      expectMutations(found, q"List(1, 2).filterNot(filterNotFunc)", q"List(1, 2).filter(filterNotFunc)")
      expectMutations(
        found,
        q"List(1, 2).filterNot(filterNotFunc).filter(filterFunc)",
        q"List(1, 2).filterNot(filterNotFunc).filterNot(filterFunc)"
      )
    }

    test("should match a boolean and a conditional") {
      val tree = q"def foo = false && 15 > 4"

      val found = tree.collect(sut.allMatchers).map(_(PlaceableTree(tree.body)))

      assertEquals(found.flatMap(_.toSeq).flatMap(_.toVector).length, 5)
      expectMutations(found, q"false", q"true")("BooleanLiteral", implicitly)
      expectMutations(found, q"&&", q"||")("LogicalOperator", implicitly)
      expectMutations(found, q">", q"<", q"==")("EqualityOperator", implicitly)
    }

    test("should match the default case of a constructor argument") {
      val tree = q"class Person(isOld: Boolean = 18 > 15) { }"

      val found = tree.collect(sut.allMatchers).map(_(PlaceableTree(tree.find(q"18 > 15").value)))

      assertEquals(found.flatMap(_.toSeq).flatMap(_.toVector).length, 3)
      expectMutations(found, q">", q">=", q"<", q"==")("EqualityOperator", implicitly)
    }

    test("should match on the default case of a function argument") {
      val tree = q"def hasGoodBack(isOld: Boolean = age > 60): Boolean = isOld"

      val found = tree.collect(sut.allMatchers).map(_(PlaceableTree(tree.find(q"age > 60").value)))

      assertEquals(found.flatMap(_.toSeq).flatMap(_.toVector).length, 3)
      expectMutations(found, q">", q">=", q"<", q"==")("EqualityOperator", implicitly)
    }
  }

  describe("matchEqualityOperator matcher") {
    implicit val mutatorName = "EqualityOperator"
    test("should match >= sign with >, <, and ==") {
      val tree = q"def foo = 18 >= 20"
      val found = tree.collect(sut.matchEqualityOperator).map(_(PlaceableTree(tree.body)))
      expectMutations(
        found,
        GreaterThanEqualTo.tree,
        GreaterThan.tree,
        LesserThan.tree,
        EqualTo.tree
      )
    }

    test("should match > with >=, < and ==") {
      val tree = q"def foo = 18 > 20"
      val found = tree.collect(sut.matchEqualityOperator).map(_(PlaceableTree(tree.body)))
      expectMutations(
        found,
        GreaterThan.tree,
        GreaterThanEqualTo.tree,
        LesserThan.tree,
        EqualTo.tree
      )
    }

    test("should match <= to <, >= and ==") {
      val tree = q"def foo = 18 <= 20"
      val found = tree.collect(sut.matchEqualityOperator).map(_(PlaceableTree(tree.body)))
      expectMutations(
        found,
        LesserThanEqualTo.tree,
        LesserThan.tree,
        GreaterThanEqualTo.tree,
        EqualTo.tree
      )
    }

    test("should match < to <=, > and ==") {
      val tree = q"def foo = 18 < 20"
      val found = tree.collect(sut.matchEqualityOperator).map(_(PlaceableTree(tree.body)))
      expectMutations(
        found,
        LesserThan.tree,
        LesserThanEqualTo.tree,
        GreaterThan.tree,
        EqualTo.tree
      )
    }

    test("should match == to !=") {
      val tree = q"def foo = 18 == 20"
      val found = tree.collect(sut.matchEqualityOperator).map(_(PlaceableTree(tree.body)))
      expectMutations(
        found,
        EqualTo.tree,
        NotEqualTo.tree
      )
    }

    test("should match != to ==") {
      expectMutations(
        sut.matchEqualityOperator,
        q"def foo = 18 != 20",
        NotEqualTo.tree,
        EqualTo.tree
      )
    }

    test("should match === to =!=") {
      expectMutations(
        sut.matchEqualityOperator,
        q"def foo = 18 === 20",
        TypedEqualTo.tree,
        TypedNotEqualTo.tree
      )
    }

    test("should match =!= to ===") {
      expectMutations(
        sut.matchEqualityOperator,
        q"def foo = 18 =!= 20",
        TypedNotEqualTo.tree,
        TypedEqualTo.tree
      )
    }
  }
  describe("matchLogicalOperator matcher") {
    implicit val mutatorName = "LogicalOperator"

    test("should match && to ||") {
      expectMutations(
        sut.matchLogicalOperator,
        q"def foo = a && b",
        And.tree,
        Or.tree
      )
    }

    test("should match || to &&") {
      expectMutations(
        sut.matchLogicalOperator,
        q"def foo = a || b",
        Or.tree,
        And.tree
      )
    }
  }

  describe("matchMethodExpression matcher") {
    test("should match filter to filterNot") {
      expectedMutations(sut.matchMethodExpression, q"def foo = List(1, 2, 3).filter(_ % 2 == 0)", FilterNot)
    }

    test("should match filterNot to filter") {
      expectedMutations(sut.matchMethodExpression, q"def foo = List(1, 2, 3).filterNot(_ % 2 == 0)", Filter)
    }

    test("should match exists to forall") {
      expectedMutations(sut.matchMethodExpression, q"def foo = List(1, 2, 3).exists(_ % 2 == 0)", Forall)
    }

    test("should match forall to exists") {
      expectedMutations(sut.matchMethodExpression, q"def foo = List(1, 2, 3).forall(_ % 2 == 0)", Exists)
    }

    test("should match take to drop") {
      expectedMutations(sut.matchMethodExpression, q"def foo = List(1, 2, 3).take(2)", Drop)
    }

    test("should match drop to take") {
      expectedMutations(sut.matchMethodExpression, q"def foo = List(1, 2, 3).drop(2)", Take)
    }

    test("should match takeRight to dropRight") {
      expectedMutations(sut.matchMethodExpression, q"def foo = List(1, 2, 3).takeRight(2)", DropRight)
    }

    test("should match dropRight to takeRight") {
      expectedMutations(sut.matchMethodExpression, q"def foo = List(1, 2, 3).dropRight(2)", TakeRight)
    }

    test("should match takeWhile to dropWhile") {
      expectedMutations(sut.matchMethodExpression, q"def foo = List(1, 2, 3).dropWhile(_ < 2)", TakeWhile)
    }

    test("should match dropWhile to takeWhile") {
      expectedMutations(sut.matchMethodExpression, q"def foo = List(1, 2, 3).takeWhile(_ < 2)", DropWhile)
    }

    test("should match isEmpty to nonEmpty") {
      expectedMutations(sut.matchMethodExpression, q"def foo = List(1, 2, 3).isEmpty", NonEmpty)
    }

    test("should match nonEmpty to isEmpty") {
      expectedMutations(sut.matchMethodExpression, q"def foo = List(1, 2, 3).nonEmpty", IsEmpty)
    }

    test("should match indexOf to lastIndexOf") {
      expectedMutations(sut.matchMethodExpression, q"def foo = List(1, 2, 3).indexOf(2)", LastIndexOf)
    }

    test("should match lastIndexOf to indexOf") {
      expectedMutations(sut.matchMethodExpression, q"def foo = List(1, 2, 3).lastIndexOf(2)", IndexOf)
    }

    test("should match max to min") {
      expectedMutations(sut.matchMethodExpression, q"def foo = List(1, 2, 3).max", Min)
    }

    test("should match min to max") {
      expectedMutations(sut.matchMethodExpression, q"def foo = List(1, 2, 3).min", Max)
    }

    test("should match maxBy to minBy") {
      expectedMutations(sut.matchMethodExpression, q"def foo = List(1, 2, 3).maxBy(_.toString)", MinBy)
    }

    test("should match minBy to maxBy") {
      expectedMutations(sut.matchMethodExpression, q"def foo = List(1, 2, 3).minBy(_.toString)", MaxBy)
    }
  }

  describe("matchBooleanLiteral matcher") {
    implicit val mutatorName = "BooleanLiteral"

    test("should match false to true") {
      expectMutations(
        sut.matchBooleanLiteral,
        q"def foo = false",
        False.tree,
        True.tree
      )
    }

    test("should match true to false") {
      expectMutations(
        sut.matchBooleanLiteral,
        q"def foo = true",
        True.tree,
        False.tree
      )
    }
  }
  describe("matchStringLiteral matcher") {
    implicit val mutatorName = "StringLiteral"

    test("should match foo to NonEmptyString") {
      expectMutations(
        sut.matchStringLiteral,
        q"""def foo: String = "bar"""",
        Lit.String("bar"),
        EmptyString.tree
      )
    }

    test("should match empty string to StrykerWasHere") {
      expectMutations(
        sut.matchStringLiteral,
        q"""def foo = "" """,
        EmptyString.tree,
        StrykerWasHereString.tree
      )
    }

    test("should match on interpolated strings") {
      val interpolated =
        Term.Interpolate(q"s", List(Lit.String("interpolate "), Lit.String("")), List(q"foo"))
      val tree = q"def foo = $interpolated"
      val emptyString = Lit.String("")

      assertEquals(interpolated.syntax, "s\"interpolate $foo\"")
      expectMutations(
        sut.matchStringLiteral,
        tree,
        interpolated,
        emptyString
      )
    }

    test("should match once on interpolated strings with multiple parts") {
      val interpolated =
        Term.Interpolate(
          q"s",
          List(Lit.String("interpolate "), Lit.String(" foo "), Lit.String(" bar")),
          List(q"fooVar", q"barVar + 1")
        )
      val tree = q"def foo = $interpolated"
      val emptyString = Lit.String("")

      assertEquals(interpolated.syntax, "s\"interpolate $fooVar foo ${barVar" + " + 1} bar\"")
      expectMutations(
        sut.matchStringLiteral,
        tree,
        interpolated,
        emptyString
      )
    }

    test("should not match non-string interpolation") {
      val interpolated =
        Term.Interpolate(q"q", List(Lit.String("interpolate "), Lit.String("")), List(q"foo"))
      val tree = q"def foo = $interpolated "

      val result = tree collect sut.allMatchers

      assertEquals(interpolated.syntax, "q\"interpolate $foo\"")
      assert(result.isEmpty, result)
    }

    test("should not match pattern interpolation") {
      val tree = source"""class Foo {
        def bar = {
          case t"interpolate" => _
        }
      }"""

      val result = tree collect sut.allMatchers

      assert(result.isEmpty, result)
    }

    test("should match pattern in string") {
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

    test("should not match xml literals") {
      val tree = source"""class Foo {
        def bar = ${Term.Xml(List(Lit.String("<foo>"), Lit.String("</foo>")), List(q"foo"))}
      }"""

      val result = tree collect sut.allMatchers

      assert(result.isEmpty, result)
    }

    test("should not match empty strings on xml literals") {
      val tree = source"""class Foo {
        def bar = ${Term.Xml(List(Lit.String("<foo>"), Lit.String("")), List(q"foo"))}
      }"""

      val result = tree collect sut.allMatchers

      assert(result.isEmpty, result)
    }

    test("should match inside xml literal args") {
      val str = Lit.String("str")
      val tree = q"""def bar = ${Term.Xml(List(Lit.String("<foo>"), Lit.String("</foo>")), List(str))}"""
      expectMutations(
        sut.matchStringLiteral,
        tree,
        str,
        Lit.String("")
      )
    }

    test("should not match xml interpolation") {
      val tree = Pat.Xml(List(Lit.String("<foo></xml>")), List.empty)

      val result = tree collect sut.allMatchers

      assert(result.isEmpty, result)
    }
  }

  describe("regexMutator") {
    implicit val mutatorName = "RegularExpression"
    val regex = Lit.String(".*")

    test("should match Regex constructor") {
      val tree = q"""def foo = new Regex($regex)"""

      expectMutations(
        sut.matchRegex,
        tree,
        regex,
        Lit.String(".")
      )
    }

    test("should match scala.util.matching.Regex constructor") {
      expectMutations(
        sut.matchRegex,
        q"""def foo = new scala.util.matching.Regex($regex)""",
        regex,
        Lit.String(".")
      )
    }

    test("should match a Regex constructor with named groups") {
      expectMutations(
        sut.matchRegex,
        q"""def foo = new Regex($regex, "any")""",
        regex,
        Lit.String(".")
      )
    }

    test("should match Regex String ops") {
      expectMutations(
        sut.matchRegex,
        q"""def foo = $regex.r""",
        regex,
        Lit.String(".")
      )
    }

    test("should match Pattern.compile Regex constructor") {
      expectMutations(
        sut.matchRegex,
        q"""def foo = Pattern.compile($regex)""",
        regex,
        Lit.String(".")
      )
    }

    test("should match java.util.regex.Pattern.compile Regex constructor") {
      expectMutations(
        sut.matchRegex,
        q"""def foo = java.util.regex.Pattern.compile($regex)""",
        regex,
        Lit.String(".")
      )
    }

    test("should match Pattern.compile Regex constructor with flags") {
      expectMutations(
        sut.matchRegex,
        q"""def foo = Pattern.compile($regex, CASE_INSENSITIVE)""",
        regex,
        Lit.String(".")
      )
    }

    test("should not match e regular string") {
      expectMutations(
        sut.matchRegex,
        q"""def foo = $regex""",
        regex
      )
    }

    test("should handle regexes without any mutations") {
      val regex = Lit.String("(a|b)")
      val tree = q"""def foo = new Regex($regex)"""
      val result = tree.collect(sut.matchRegex).loneElement.apply(PlaceableTree(tree.body)).leftValue.loneElement

      assertEquals(result._1.mutatedStatement, regex)
      assertEquals(result._2.explanation, NoRegexMutationsFound(regex.value).explanation)
    }

  }

  describe("no function name matching") {
    test("should not match a function with a mutator name") {
      val tree = q"def isEmpty = foo"

      val result = tree collect sut.allMatchers

      assert(result.isEmpty, result)
    }

    test("should not match on a case class with a mutator name") {
      val tree = q"case class indexOf(foo: String)"

      val result = tree collect sut.allMatchers

      assert(result.isEmpty, result)
    }

    test("should not match on a variable with a mutator name") {
      val tree = q"val min = 5"

      val result = tree collect sut.allMatchers

      assert(result.isEmpty, result)
    }

    test("should not match on type arguments") {
      val tree = t"String Refined StartsWith[${Lit.String("jdbc:")}]"

      val result = tree collect sut.allMatchers

      assert(result.isEmpty, result)
    }

    test("should not match on infix type arguments") {
      val tree = t"String Refined ${Lit.String("jdbc:")}"

      val result = tree collect sut.allMatchers

      assert(result.isEmpty, result)
    }

    test("should not match on type apply") {
      val tree = q"foo[${Lit.String("jdbc:")}]()"

      val result = tree collect sut.allMatchers

      assert(result.isEmpty, result)
    }

    test("should not match on literal type declarations") {
      val tree = q"val a: ${Lit.String("4")} = ???"

      val result = tree collect sut.allMatchers

      assert(result.isEmpty, result)
    }

    test("should not match on literal type declarations for var") {
      val tree = q"var a: ${Lit.String("4")} = ???"

      val result = tree collect sut.allMatchers

      assert(result.isEmpty, result)
    }

    test("should not match on infix literal type declarations") {
      val tree = q"val a: ${Lit.String("4")} + ${Lit.String("6")} = ???"

      val result = tree collect sut.allMatchers

      assert(result.isEmpty, result)
    }

    test("should not match on def literal return types") {
      val tree = q"def a: ${Lit.String("4")} = ???"

      val result = tree collect sut.allMatchers

      assert(result.isEmpty, result)
    }

    test("should not match on literal function types") {
      val tree = q"def a: (Int => ${Lit.String("4")}) = ???"

      val result = tree collect sut.allMatchers

      assert(result.isEmpty, result)
    }

    test("should not match on type aliases") {
      val tree = q"type Foo = ${Lit.String("4")}"

      val result = tree collect sut.allMatchers

      assert(result.isEmpty, result)
    }

    test("should not match a function with a single expression") {
      val tree = q"def isEmpty = exists"

      val result = tree
        .collect(sut.allMatchers)
        .map(_(PlaceableTree(tree.body)))
        .flatMap(_.toSeq)
        .flatMap(_.toVector)
        .map(_.metadata)

      assert(!result.map(_.original).contains("exists"))
    }

    test("should mutate if statements with true and false as condition") {
      implicit val mutatorName = "ConditionalExpression"
      val tree = q"if(aVariable) { println }"

      val found = tree.collect(sut.allMatchers).map(_(PlaceableTree(tree)))

      assertEquals(found.flatMap(_.toSeq).flatMap(_.toVector).length, 2)
      expectMutations(found, q"aVariable", q"true")
      expectMutations(found, q"aVariable", q"false")
    }

    test("should mutate while statements with false as condition") {
      implicit val mutatorName = "ConditionalExpression"
      val tree = q"while(aVariable) { println }"

      val found = tree.collect(sut.allMatchers).map(_(PlaceableTree(tree)))

      assertEquals(found.flatMap(_.toSeq).flatMap(_.toVector).length, 1)
      expectMutations(found, q"aVariable", q"false")
    }

    test("should mutate do while statements with false as condition") {
      implicit val mutatorName = "ConditionalExpression"
      val tree = q"do { println } while(aVariable)"

      val found = tree.collect(sut.allMatchers).map(_(PlaceableTree(tree)))

      assertEquals(found.flatMap(_.toSeq).flatMap(_.toVector).length, 1)
      expectMutations(found, q"aVariable", q"false")
    }

    test("should mutate conditional statements that have a literal boolean as condition only once") {
      implicit val mutatorName = "BooleanLiteral"
      val trueTree = q"if(true) { println }"
      val falseTree = q"if(false) { println }"

      val trueFound = trueTree.collect(sut.allMatchers).map(_(PlaceableTree(trueTree)))
      val falseFound = falseTree.collect(sut.allMatchers).map(_(PlaceableTree(falseTree)))

      assertEquals(trueFound.length, 1)
      assertEquals(falseFound.length, 1)
      expectMutations(trueFound, q"true", q"false")
      expectMutations(falseFound, q"false", q"true")
    }
  }

  describe("filtering") {
    import cats.syntax.all.*
    test("should filter out config excluded mutants") {
      implicit val conf: Config = Config.default.copy(excludedMutations = Set("LogicalOperator"))
      val sut = new MutantMatcherImpl()(conf)
      val tree = q"def foo = 15 > 20 && 20 < 15"

      val (ignored, found) = tree.collect(sut.allMatchers).map(_(PlaceableTree(tree.body))).partitionEither(identity)

      assertEquals(found.flatMap(_.toVector).length, 6)
      val (code, reason) = ignored.flatMap(_.toVector).loneElement

      assertEquals(reason, MutationExcluded)
      assertEquals(code.mutatedStatement, q"15 > 20 || 20 < 15")
      assertEquals(code.metadata.original, "&&")
      assertEquals(code.metadata.replacement, "||")
    }

    test("should filter out string mutants inside annotations") {
      val tree = q"""@SuppressWarnings(Array("stryker4s.mutation.StringLiteral"))
                      val x = { val l = "s3"; l }"""

      val (ignored, found) =
        tree
          .collect(sut.allMatchers)
          .take(1)
          .map(_(PlaceableTree(tree.find(Lit.String("stryker4s.mutation.StringLiteral")).value)))
          .partitionEither(identity)

      assertEquals(found.flatMap(_.toVector), List.empty)
      val (code, reason) = ignored.flatMap(_.toVector).loneElement

      assertEquals(reason, MutationExcluded)
      assertEquals(code.metadata.original, "\"stryker4s.mutation.StringLiteral\"")
      assertEquals(code.metadata.replacement, "\"\"")
    }

    test("should log partition unparsable regular expressions") {
      val regex = Lit.String("[[]]")
      val tree = q"""def foobar = new Regex($regex)"""

      val (ignored, found) =
        tree.collect(sut.allMatchers).map(_(PlaceableTree(tree.body))).partitionEither(identity)

      assertEquals(found.flatMap(_.toVector), List.empty)
      val (_, reason) = ignored.flatMap(_.toVector).loneElement

      assertEquals(reason, RegexParseError("[[]]", "[Error] Parser: Position 1:1, found \"[[]]\""))
    }
  }
}
