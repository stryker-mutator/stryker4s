package stryker4s.mutants.tree

import cats.data.{NonEmptyList, NonEmptyVector}
import fs2.io.file.Path
import stryker4s.extension.TreeExtensions.*
import stryker4s.extension.exception.UnableToBuildPatternMatchException
import stryker4s.extension.mutationtype.{ConditionalTrue, GreaterThan, Mutation, True}
import stryker4s.model.*
import stryker4s.mutants.applymutants.ActiveMutationContext
import stryker4s.testkit.{LogMatchers, Stryker4sSuite}
import stryker4s.testutil.TestData

import scala.meta.*

class MutantInstrumenterTest extends Stryker4sSuite with TestData with LogMatchers {

  val path = Path("foo/bar.scala")
  val separator = path.toNioPath.getFileSystem().getSeparator()

  describe("instrumentFile") {
    test("should transform 2 mutations into a match statement with 2 mutated and 1 original") {
      // Arrange
      val source = source"""class Foo { def foo = x >= 15 }"""
      val originalStatement = source.find(q"x >= 15").value
      val context = SourceContext(source, path)
      val mutants = Map(
        PlaceableTree(originalStatement) ->
          toMutations(originalStatement, GreaterThan, q"x > 15", q"x <= 15")
      )
      val sut = new MutantInstrumenter(InstrumenterOptions.testRunner)

      //   // Act
      val mutatedSource = sut.instrumentFile(context, mutants).mutatedSource
      val result = mutatedSource.collectFirst { case t: Term.Match => t }.value

      //   // Assert
      assertEquals(result.expr, q"_root_.stryker4s.activeMutation")
      assertEquals(
        result.cases.map(_.syntax),
        List(
          p"case 0 => x > 15".syntax,
          p"case 1 => x <= 15".syntax,
          p"case _ if _root_.stryker4s.coverage.coverMutant(0, 1) => x >= 15".syntax
        )
      )
      val expected = source"""class Foo {
                   def foo = _root_.stryker4s.activeMutation match {
                     case 0 =>
                       x > 15
                     case 1 =>
                       x <= 15
                     case _ if _root_.stryker4s.coverage.coverMutant(0, 1) =>
                       x >= 15
                   }
                 }"""
      assertEquals(mutatedSource, expected)
      assertNotLoggedError("Failed to instrument mutants")
    }

    test("should place mutants on the correct statement even if the name appears twice") {
      val source = source"""class Foo {
        def foo = {
          val bar = true
          if (bar) 1 else 2
        }
      }"""
      val bars = source.collect { case f @ Term.Name("bar") => f }
      // bars should have length 2
      val originalStatement = bars.last

      val context = SourceContext(source, path)
      val mutants = Map(
        PlaceableTree(originalStatement) -> toMutations(originalStatement, ConditionalTrue, q"true", q"false")
      )
      val sut = new MutantInstrumenter(InstrumenterOptions.testRunner)

      //   // Act
      val mutatedSource = sut.instrumentFile(context, mutants).mutatedSource
      val result = mutatedSource.collectFirst { case t: Term.Match => t }.value

      //   // Assert
      assertEquals(result.expr, q"_root_.stryker4s.activeMutation")
      assertEquals(
        result.cases.map(_.syntax),
        List(
          p"case 0 => true".syntax,
          p"case 1 => false".syntax,
          p"case _ if _root_.stryker4s.coverage.coverMutant(0, 1) => bar".syntax
        )
      )
      val expected = source"""class Foo {
        def foo = {
          val bar = true
          if (
            _root_.stryker4s.activeMutation match {
              case 0 =>
                true
              case 1 =>
                false
              case _ if _root_.stryker4s.coverage.coverMutant(0, 1) =>
                bar
            }
          ) 1
          else 2
        }
      }"""
      assertEquals(mutatedSource, expected)
    }

    test("should apply the correct instrumenter options") {
      val source = source"""class Foo { def foo = x >= 15 }"""
      val originalStatement = source.find(q"x >= 15").value
      val context = SourceContext(source, path)
      val mutants = Map(
        PlaceableTree(originalStatement) ->
          toMutations(originalStatement, GreaterThan, q"x > 15", q"x <= 15")
      )
      val sut = new MutantInstrumenter(InstrumenterOptions.sysContext(ActiveMutationContext.envVar))

      //   // Act
      val result = sut.instrumentFile(context, mutants).mutatedSource.collectFirst { case t: Term.Match => t }.value

      //   // Assert
      assertEquals(result.expr, q"_root_.scala.sys.env.get(${Lit.String("ACTIVE_MUTATION")})")
      assertEquals(result.cases.head.pat, p"Some(${Lit.String("0")})")
      assertEquals(result.cases.map(_.syntax).last, p"case _ => x >= 15".syntax)
    }

    // describe("buildNewSource") {
    test("should log failures correctly") {
      // Arrange
      val source = """class Foo { def foo = true }""".parse[Source].get
      val original = source.find(q"true").value
      val context = SourceContext(source, path)
      val mutants = Map(PlaceableTree(original) -> toMutations(original, True, q"false", q"true"))

      val sut = new MutantInstrumenter(InstrumenterOptions.testRunner) {
        override def buildMatch(cases: NonEmptyVector[Case]): Nothing =
          throw new Exception()
      }

      // Act
      interceptMessage[UnableToBuildPatternMatchException](
        s"""Failed to instrument mutants in `$path`.
           |Please open an issue on github and include the stacktrace and failed instrumentation code: https://github.com/stryker-mutator/stryker4s/issues/new""".stripMargin
      )(sut.instrumentFile(context, mutants))

      // Assert
      assertLoggedError(s"Failed to instrument mutants in `foo${separator}bar.scala`. Original statement: [true]")
      assertLoggedError("Failed mutation(s) '0, 1' at Input.String(\"class Foo { def foo = true }\"):1:23")
      assertLoggedError("This is likely an issue on Stryker4s's end, please take a look at the debug logs")
    }

    test("should rethrow Stryker4sExceptions") {
      val source = """class Foo { def foo = true }""".parse[Source].get
      val original = source.find(q"true").value
      val context = SourceContext(source, path)
      val mutants = Map(PlaceableTree(original) -> toMutations(original, True, q"false", q"true"))

      val expectedException = UnableToBuildPatternMatchException(path)
      val sut = new MutantInstrumenter(InstrumenterOptions.testRunner) {
        override def defaultCase(placeableTree: PlaceableTree, mutantIds: NonEmptyList[MutantId]): Nothing =
          throw expectedException
      }

      // Act
      val result = intercept[UnableToBuildPatternMatchException](sut.instrumentFile(context, mutants))
      assertEquals(result, expectedException)
    }

  }

  def toMutations[T <: Tree](
      original: Term,
      category: Mutation[T],
      firstReplacement: Term,
      replacements: Term*
  ): MutantsWithId = {
    NonEmptyVector
      .of(firstReplacement, replacements*)
      .zipWithIndex
      .map { case (replacement, id) =>
        MutantWithId(
          MutantId(id),
          MutatedCode(
            replacement,
            MutantMetadata(original.toString(), replacement.toString, category.mutationName, original.pos)
          )
        )
      }
  }
}
