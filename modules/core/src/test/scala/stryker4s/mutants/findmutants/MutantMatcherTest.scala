package stryker4s.mutants.findmutants

import munit.Location
import stryker4s.config.{Config, ExcludedMutation}
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
      val tree = "def foo = 15 > 20 && 20 < 15".parseDef
      val found = tree.collect(sut.allMatchers).map(_(PlaceableTree(tree.body)))

      assertEquals(found.flatMap(_.toSeq).flatMap(_.toVector).length, 7)
      expectMutations(found, Term.Name(">"), Term.Name(">="), Term.Name("<"), Term.Name("=="))(
        "EqualityOperator",
        implicitly
      )
      expectMutations(found, Term.Name("&&"), Term.Name("||"))("LogicalOperator", implicitly)
      expectMutations(found, Term.Name("<"), Term.Name("<="), Term.Name(">"), Term.Name("=="))(
        "EqualityOperator",
        implicitly
      )
    }

    test("should match a method") {
      implicit val mutatorName = "MethodExpression"
      val tree = "def foo = List(1, 2).filterNot(filterNotFunc).filter(filterFunc)".parseDef
      val found = tree.collect(sut.allMatchers).map(_(PlaceableTree(tree.body)))

      assertEquals(found.length, 2)
      expectMutations(
        found,
        "List(1, 2).filterNot(filterNotFunc)".parseTerm,
        "List(1, 2).filter(filterNotFunc)".parseTerm
      )
      expectMutations(
        found,
        "List(1, 2).filterNot(filterNotFunc).filter(filterFunc)".parseTerm,
        "List(1, 2).filterNot(filterNotFunc).filterNot(filterFunc)".parseTerm
      )
    }

    test("should match a boolean and a conditional") {
      val tree = "def foo = false && 15 > 4".parseDef

      val found = tree.collect(sut.allMatchers).map(_(PlaceableTree(tree.body)))

      assertEquals(found.flatMap(_.toSeq).flatMap(_.toVector).length, 5)
      expectMutations(found, Lit.Boolean(false), Lit.Boolean(true))("BooleanLiteral", implicitly)
      expectMutations(found, Term.Name("&&"), Term.Name("||"))("LogicalOperator", implicitly)
      expectMutations(found, Term.Name(">"), Term.Name("<"), Term.Name("=="))("EqualityOperator", implicitly)
    }

    test("should match the default case of a constructor argument") {
      val tree = "class Person(isOld: Boolean = 18 > 15) { }".parseStat

      val found = tree.collect(sut.allMatchers).map(_(PlaceableTree(tree.find("18 > 15".parseTerm).value)))

      assertEquals(found.flatMap(_.toSeq).flatMap(_.toVector).length, 3)
      expectMutations(found, Term.Name(">"), Term.Name(">="), Term.Name("<"), Term.Name("=="))(
        "EqualityOperator",
        implicitly
      )
    }

    test("should match on the default case of a function argument") {
      val tree = "def hasGoodBack(isOld: Boolean = age > 60): Boolean = isOld".parseDef

      val found = tree.collect(sut.allMatchers).map(_(PlaceableTree(tree.find("age > 60".parseTerm).value)))

      assertEquals(found.flatMap(_.toSeq).flatMap(_.toVector).length, 3)
      expectMutations(found, Term.Name(">"), Term.Name(">="), Term.Name("<"), Term.Name("=="))(
        "EqualityOperator",
        implicitly
      )
    }
  }

  describe("matchEqualityOperator matcher") {
    implicit val mutatorName = "EqualityOperator"
    test("should match >= sign with >, <, and ==") {
      val tree = "def foo = 18 >= 20".parseDef
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
      val tree = "def foo = 18 > 20".parseDef
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
      val tree = "def foo = 18 <= 20".parseDef
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
      val tree = "def foo = 18 < 20".parseDef
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
      val tree = "def foo = 18 == 20".parseDef
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
        "def foo = 18 != 20".parseDef,
        NotEqualTo.tree,
        EqualTo.tree
      )
    }

    test("should match === to =!=") {
      expectMutations(
        sut.matchEqualityOperator,
        "def foo = 18 === 20".parseDef,
        TypedEqualTo.tree,
        TypedNotEqualTo.tree
      )
    }

    test("should match =!= to ===") {
      expectMutations(
        sut.matchEqualityOperator,
        "def foo = 18 =!= 20".parseDef,
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
        "def foo = a && b".parseDef,
        And.tree,
        Or.tree
      )
    }

    test("should match || to &&") {
      expectMutations(
        sut.matchLogicalOperator,
        "def foo = a || b".parseDef,
        Or.tree,
        And.tree
      )
    }
  }

  describe("matchMethodExpression matcher") {
    test("should match filter to filterNot") {
      expectedMutations(sut.matchMethodExpression, "def foo = List(1, 2, 3).filter(_ % 2 == 0)".parseDef, FilterNot)
    }

    test("should match filterNot to filter") {
      expectedMutations(sut.matchMethodExpression, "def foo = List(1, 2, 3).filterNot(_ % 2 == 0)".parseDef, Filter)
    }

    test("should match exists to forall") {
      expectedMutations(sut.matchMethodExpression, "def foo = List(1, 2, 3).exists(_ % 2 == 0)".parseDef, Forall)
    }

    test("should match forall to exists") {
      expectedMutations(sut.matchMethodExpression, "def foo = List(1, 2, 3).forall(_ % 2 == 0)".parseDef, Exists)
    }

    test("should match take to drop") {
      expectedMutations(sut.matchMethodExpression, "def foo = List(1, 2, 3).take(2)".parseDef, Drop)
    }

    test("should match drop to take") {
      expectedMutations(sut.matchMethodExpression, "def foo = List(1, 2, 3).drop(2)".parseDef, Take)
    }

    test("should match takeRight to dropRight") {
      expectedMutations(sut.matchMethodExpression, "def foo = List(1, 2, 3).takeRight(2)".parseDef, DropRight)
    }

    test("should match dropRight to takeRight") {
      expectedMutations(sut.matchMethodExpression, "def foo = List(1, 2, 3).dropRight(2)".parseDef, TakeRight)
    }

    test("should match takeWhile to dropWhile") {
      expectedMutations(sut.matchMethodExpression, "def foo = List(1, 2, 3).dropWhile(_ < 2)".parseDef, TakeWhile)
    }

    test("should match dropWhile to takeWhile") {
      expectedMutations(sut.matchMethodExpression, "def foo = List(1, 2, 3).takeWhile(_ < 2)".parseDef, DropWhile)
    }

    test("should match isEmpty to nonEmpty") {
      expectedMutations(sut.matchMethodExpression, "def foo = List(1, 2, 3).isEmpty".parseDef, NonEmpty)
    }

    test("should match nonEmpty to isEmpty") {
      expectedMutations(sut.matchMethodExpression, "def foo = List(1, 2, 3).nonEmpty".parseDef, IsEmpty)
    }

    test("should match indexOf to lastIndexOf") {
      expectedMutations(sut.matchMethodExpression, "def foo = List(1, 2, 3).indexOf(2)".parseDef, LastIndexOf)
    }

    test("should match lastIndexOf to indexOf") {
      expectedMutations(sut.matchMethodExpression, "def foo = List(1, 2, 3).lastIndexOf(2)".parseDef, IndexOf)
    }

    test("should match max to min") {
      expectedMutations(sut.matchMethodExpression, "def foo = List(1, 2, 3).max".parseDef, Min)
    }

    test("should match min to max") {
      expectedMutations(sut.matchMethodExpression, "def foo = List(1, 2, 3).min".parseDef, Max)
    }

    test("should match maxBy to minBy") {
      expectedMutations(sut.matchMethodExpression, "def foo = List(1, 2, 3).maxBy(_.toString)".parseDef, MinBy)
    }

    test("should match minBy to maxBy") {
      expectedMutations(sut.matchMethodExpression, "def foo = List(1, 2, 3).minBy(_.toString)".parseDef, MaxBy)
    }
  }

  describe("matchBooleanLiteral matcher") {
    implicit val mutatorName = "BooleanLiteral"

    test("should match false to true") {
      expectMutations(
        sut.matchBooleanLiteral,
        "def foo = false".parseDef,
        False.tree,
        True.tree
      )
    }

    test("should match true to false") {
      expectMutations(
        sut.matchBooleanLiteral,
        "def foo = true".parseDef,
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
        """def foo: String = "bar"""".parseDef,
        Lit.String("bar"),
        EmptyString.tree
      )
    }

    test("should match empty string to StrykerWasHere") {
      expectMutations(
        sut.matchStringLiteral,
        """def foo = "" """.parseDef,
        EmptyString.tree,
        StrykerWasHereString.tree
      )
    }

    test("should match on interpolated strings") {
      val interpolated =
        Term.Interpolate(Term.Name("s"), List(Lit.String("interpolate "), Lit.String("")), List(Term.Name("foo")))
      val tree = s"def foo = ${interpolated.syntax}".parseDef
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
      val interpolated = Term.Interpolate(
        Term.Name("s"),
        List(Lit.String("interpolate "), Lit.String(" foo "), Lit.String(" bar")),
        List(Term.Name("fooVar"), "barVar + 1".parseTerm)
      )
      val tree = Defn.Def.After_4_7_3(Nil, Term.Name("foo"), Nil, None, interpolated)
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
        Term.Interpolate(Term.Name("q"), List(Lit.String("interpolate "), Lit.String("")), List(Term.Name("foo")))
      val tree = s"def foo = ${interpolated.syntax} ".parseStat

      val result = tree collect sut.allMatchers

      assertEquals(interpolated.syntax, "q\"interpolate $foo\"")
      assert(result.isEmpty, result)
    }

    test("should not match pattern interpolation") {
      val tree = """class Foo {
        def bar = {
          case t"interpolate" => _
        }
      }""".parseSource

      val result = tree collect sut.allMatchers

      assert(result.isEmpty, result)
    }

    test("should match pattern in string") {
      val tree = """def bar = {
          case "str" => 4
        }""".parseDef
      expectMutations(
        sut.matchStringLiteral,
        tree,
        Lit.String("str"),
        Lit.String("")
      )
    }

    test("should not match xml literals") {
      val tree = s"""class Foo {
        def bar = ${Term.Xml(List(Lit.String("<foo>"), Lit.String("</foo>")), List(Term.Name("foo"))).syntax}
      }""".parseSource

      val result = tree collect sut.allMatchers

      assert(result.isEmpty, result)
    }

    test("should not match empty strings on xml literals") {
      val tree = s"""class Foo {
        def bar = <foo>{foo}</foo>
      }""".parseSource

      val result = tree collect sut.allMatchers

      assert(result.isEmpty, result)
    }

    test("should match inside xml literal args") {
      val str = Lit.String("str")
      val tree =
        s"""def bar = ${Term.Xml(List(Lit.String("<foo>"), Lit.String("</foo>")), List(str)).syntax}""".parseDef
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
      val tree = s"""def foo = new Regex(".*")""".parseDef

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
        """def foo = new scala.util.matching.Regex(".*")""".parseDef,
        regex,
        Lit.String(".")
      )
    }

    test("should match a Regex constructor with named groups") {
      expectMutations(
        sut.matchRegex,
        """def foo = new Regex(".*", "any")""".parseDef,
        regex,
        Lit.String(".")
      )
    }

    test("should match Regex String ops") {
      expectMutations(
        sut.matchRegex,
        """def foo = ".*".r""".parseDef,
        regex,
        Lit.String(".")
      )
    }

    test("should match Pattern.compile Regex constructor") {
      expectMutations(
        sut.matchRegex,
        """def foo = Pattern.compile(".*")""".parseDef,
        regex,
        Lit.String(".")
      )
    }

    test("should match java.util.regex.Pattern.compile Regex constructor") {
      expectMutations(
        sut.matchRegex,
        """def foo = java.util.regex.Pattern.compile(".*")""".parseDef,
        regex,
        Lit.String(".")
      )
    }

    test("should match Pattern.compile Regex constructor with flags") {
      expectMutations(
        sut.matchRegex,
        """def foo = Pattern.compile(".*", CASE_INSENSITIVE)""".parseDef,
        regex,
        Lit.String(".")
      )
    }

    test("should not match e regular string") {
      expectMutations(
        sut.matchRegex,
        """def foo = ".*"""".parseDef,
        regex
      )
    }

    test("should handle regexes without any mutations") {
      val regex = Lit.String("(a|b)")
      val tree = """def foo = new Regex("(a|b)")""".parseDef
      val result = tree.collect(sut.matchRegex).loneElement.apply(PlaceableTree(tree.body)).leftValue.loneElement

      assertEquals(result._1.mutatedStatement, regex)
      assertEquals(result._2.explanation, NoRegexMutationsFound(regex.value).explanation)
    }

  }

  describe("no function name matching") {
    test("should not match a function with a mutator name") {
      val tree = "def isEmpty = foo".parseDef

      val result = tree collect sut.allMatchers

      assert(result.isEmpty, result)
    }

    test("should not match on a case class with a mutator name") {
      val tree = "case class indexOf(foo: String)".parseStat

      val result = tree collect sut.allMatchers

      assert(result.isEmpty, result)
    }

    test("should not match on a variable with a mutator name") {
      val tree = "val min = 5".parseStat

      val result = tree collect sut.allMatchers

      assert(result.isEmpty, result)
    }

    test("should not match on type arguments") {
      val tree = "String Refined StartsWith[\"jdbc:\"]".parseType

      val result = tree collect sut.allMatchers

      assert(result.isEmpty, result)
    }

    test("should not match on infix type arguments") {
      val tree = "String Refined \"jdbc:\"".parseType

      val result = tree collect sut.allMatchers

      assert(result.isEmpty, result)
    }

    test("should not match on type apply") {
      val tree = "foo[\"jdbc:\"]()".parseStat

      val result = tree collect sut.allMatchers

      assert(result.isEmpty, result)
    }

    test("should not match on literal type declarations") {
      val tree = "val a: \"4\" = ???".parseStat

      val result = tree collect sut.allMatchers

      assert(result.isEmpty, result)
    }

    test("should not match on literal type declarations for var") {
      val tree = "var a: \"4\" = ???".parseStat

      val result = tree collect sut.allMatchers

      assert(result.isEmpty, result)
    }

    test("should not match on infix literal type declarations") {
      val tree = "val a: \"4\" + \"6\" = ???".parseStat

      val result = tree collect sut.allMatchers

      assert(result.isEmpty, result)
    }

    test("should not match on def literal return types") {
      val tree = "def a: \"4\" = ???".parseDef

      val result = tree collect sut.allMatchers

      assert(result.isEmpty, result)
    }

    test("should not match on literal function types") {
      val tree = "def a: (Int => \"4\") = ???".parseDef

      val result = tree collect sut.allMatchers

      assert(result.isEmpty, result)
    }

    test("should not match on type aliases") {
      val tree = "type Foo = \"4\"".parseStat

      val result = tree collect sut.allMatchers

      assert(result.isEmpty, result)
    }

    test("should not match a function with a single expression") {
      val tree = "def isEmpty = exists".parseDef

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
      val tree = "if(aVariable) { println }".parseTerm

      val found = tree.collect(sut.allMatchers).map(_(PlaceableTree(tree)))

      assertEquals(found.flatMap(_.toSeq).flatMap(_.toVector).length, 2)
      expectMutations(found, Term.Name("aVariable"), Lit.Boolean(true))
      expectMutations(found, Term.Name("aVariable"), Lit.Boolean(false))
    }

    test("should mutate while statements with false as condition") {
      implicit val mutatorName = "ConditionalExpression"
      val tree = "while(aVariable) { println }".parseTerm

      val found = tree.collect(sut.allMatchers).map(_(PlaceableTree(tree)))

      assertEquals(found.flatMap(_.toSeq).flatMap(_.toVector).length, 1)
      expectMutations(found, Term.Name("aVariable"), Lit.Boolean(false))
    }

    test("should mutate do while statements with false as condition") {
      implicit val mutatorName = "ConditionalExpression"
      val tree = "do { println } while(aVariable)".parseTerm

      val found = tree.collect(sut.allMatchers).map(_(PlaceableTree(tree)))

      assertEquals(found.flatMap(_.toSeq).flatMap(_.toVector).length, 1)
      expectMutations(found, Term.Name("aVariable"), Lit.Boolean(false))
    }

    test("should mutate conditional statements that have a literal boolean as condition only once") {
      implicit val mutatorName = "BooleanLiteral"
      val trueTree = "if(true) { println }".parseTerm
      val falseTree = "if(false) { println }".parseTerm

      val trueFound = trueTree.collect(sut.allMatchers).map(_(PlaceableTree(trueTree)))
      val falseFound = falseTree.collect(sut.allMatchers).map(_(PlaceableTree(falseTree)))

      assertEquals(trueFound.length, 1)
      assertEquals(falseFound.length, 1)
      expectMutations(trueFound, Lit.Boolean(true), Lit.Boolean(false))
      expectMutations(falseFound, Lit.Boolean(false), Lit.Boolean(true))
    }
  }

  describe("filtering") {
    import cats.syntax.all.*
    test("should filter out config excluded mutants") {
      implicit val conf: Config = Config.default.copy(excludedMutations = Seq(ExcludedMutation("LogicalOperator")))
      val sut = new MutantMatcherImpl()(conf)
      val tree = "def foo = 15 > 20 && 20 < 15".parseDef

      val (ignored, found) = tree.collect(sut.allMatchers).map(_(PlaceableTree(tree.body))).partitionEither(identity)

      assertEquals(found.flatMap(_.toVector).length, 6)
      val (code, reason) = ignored.flatMap(_.toVector).loneElement

      assertEquals(reason, MutationExcluded)
      assertEquals(code.mutatedStatement, "15 > 20 || 20 < 15".parseTerm)
      assertEquals(code.metadata.original, "&&")
      assertEquals(code.metadata.replacement, "||")
    }

    test("should filter out string mutants inside annotations") {
      val tree = """@SuppressWarnings(Array("stryker4s.mutation.StringLiteral"))
                      val x = { val l = "s3"; l }""".parseStat

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
      val tree = """def foobar = new Regex("[[]]")""".parseDef

      val (ignored, found) =
        tree.collect(sut.allMatchers).map(_(PlaceableTree(tree.body))).partitionEither(identity)

      assertEquals(found.flatMap(_.toVector), List.empty)
      val (_, reason) = ignored.flatMap(_.toVector).loneElement

      assertEquals(reason, RegexParseError("[[]]", "[Error] Parser: Position 1:1, found \"[[]]\""))
    }
  }
}
