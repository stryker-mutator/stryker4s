package stryker4s.mutants.tree

import cats.data.{NonEmptyList, NonEmptyVector}
import cats.syntax.option.*
import fs2.io.file.Path
import stryker4s.exception.UnableToBuildPatternMatchException
import stryker4s.extension.TreeExtensions.*
import stryker4s.model.*
import stryker4s.mutants.applymutants.ActiveMutationContext
import stryker4s.mutation.{ConditionalTrue, GreaterThan, Mutation, True}
import stryker4s.testkit.{LogMatchers, Stryker4sSuite}
import stryker4s.testutil.TestData

import scala.meta.*

class MutantInstrumenterTest extends Stryker4sSuite with TestData with LogMatchers {

  val path = Path("foo/bar.scala")
  val separator = path.toNioPath.getFileSystem().getSeparator()

  describe("instrumentFile") {
    test("should transform 2 mutations into a match statement with 2 mutated and 1 original") {
      // Arrange
      val source = """class Foo { def foo = x >= 15 }""".parseSource
      val originalStatement = source.find("x >= 15".parseTerm).value
      val context = SourceContext(source, path)
      val mutants = Map(
        PlaceableTree(originalStatement) ->
          toMutations(originalStatement, GreaterThan, "x > 15".parseTerm, "x <= 15".parseTerm)
      )
      val sut = new MutantInstrumenter(InstrumenterOptions.testRunner)

      // Act
      val mutatedSource = sut.instrumentFile(context, mutants).mutatedSource
      val result = mutatedSource.collectFirst { case t: Term.Match => t }.value

      // Assert
      assertEquals(result.expr, "_root_.stryker4s.activeMutation".parseTerm)
      assertEquals(
        result.casesBlock.cases,
        List(
          "case 0 => x > 15".parseCase,
          "case 1 => x <= 15".parseCase,
          "case _ if _root_.stryker4s.coverage.coverMutant(0, 1) => x >= 15".parseCase
        )
      )
      val expected = """class Foo {
                   def foo = _root_.stryker4s.activeMutation match {
                     case 0 =>
                       x > 15
                     case 1 =>
                       x <= 15
                     case _ if _root_.stryker4s.coverage.coverMutant(0, 1) =>
                       x >= 15
                   }
                 }""".parseSource
      assertEquals(mutatedSource, expected)
      assertNotLoggedError("Failed to instrument mutants")
    }

    test("should place mutants on the correct statement even if the name appears twice") {
      val source = """class Foo {
        def foo = {
          val bar = true
          if (bar) 1 else 2
        }
      }""".parseSource
      val bars = source.collect { case f @ Term.Name("bar") => f }
      // bars should have length 2
      val originalStatement = bars.last

      val context = SourceContext(source, path)
      val mutants = Map(
        PlaceableTree(originalStatement) -> toMutations(
          originalStatement,
          ConditionalTrue,
          Lit.Boolean(true),
          Lit.Boolean(false)
        )
      )
      val sut = new MutantInstrumenter(InstrumenterOptions.testRunner)

      //   // Act
      val mutatedSource = sut.instrumentFile(context, mutants).mutatedSource
      val result = mutatedSource.collectFirst { case t: Term.Match => t }.value

      //   // Assert
      assertEquals(result.expr, "_root_.stryker4s.activeMutation".parseTerm)
      assertEquals(
        result.casesBlock.cases,
        List(
          "case 0 => true".parseCase,
          "case 1 => false".parseCase,
          "case _ if _root_.stryker4s.coverage.coverMutant(0, 1) => bar".parseCase
        )
      )
      val expected = """class Foo {
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
      }""".parseSource
      assertEquals(mutatedSource, expected)
    }

    test("should apply the correct instrumenter options") {
      val source = """class Foo { def foo = x >= 15 }""".parseSource
      val originalStatement = source.find("x >= 15".parseTerm).value
      val context = SourceContext(source, path)
      val mutants = Map(
        PlaceableTree(originalStatement) ->
          toMutations(originalStatement, GreaterThan, "x > 15".parseTerm, "x <= 15".parseTerm)
      )
      val sut = new MutantInstrumenter(InstrumenterOptions.sysContext(ActiveMutationContext.envVar))

      //   // Act
      val result = sut.instrumentFile(context, mutants).mutatedSource.collectFirst { case t: Term.Match => t }.value

      //   // Assert
      assertEquals(result.expr, "_root_.scala.sys.env.get(\"ACTIVE_MUTATION\")".parseTerm)
      assertEquals(
        result.casesBlock.cases,
        List(
          "case Some(\"0\") => x > 15".parseCase,
          "case Some(\"1\") => x <= 15".parseCase,
          "case _ => x >= 15".parseCase
        )
      )
    }

    // describe("buildNewSource") {
    test("should log failures correctly") {
      // Arrange
      val source = """class Foo { def foo = true }""".parse[Source].get
      val original = source.find(Lit.Boolean(true)).value
      val context = SourceContext(source, path)
      val mutants = Map(PlaceableTree(original) -> toMutations(original, True, Lit.Boolean(false), Lit.Boolean(true)))

      val sut = new MutantInstrumenter(InstrumenterOptions.testRunner) {
        override def buildMatch(cases: NonEmptyVector[Case]): Nothing =
          throw new Exception()
      }

      // Act
      val _ = interceptMessage[UnableToBuildPatternMatchException](
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
      val original = source.find(Lit.Boolean(true)).value
      val context = SourceContext(source, path)
      val mutants = Map(PlaceableTree(original) -> toMutations(original, True, Lit.Boolean(false), Lit.Boolean(true)))

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
            MutantMetadata(original.toString(), replacement.toString, category.mutationName, original.pos, none)
          )
        )
      }
  }
}
