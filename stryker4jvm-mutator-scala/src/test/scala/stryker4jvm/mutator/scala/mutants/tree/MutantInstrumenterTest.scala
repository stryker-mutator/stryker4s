package stryker4jvm.mutator.scala.mutants.tree

import cats.data.{NonEmptyList, NonEmptyVector}
import fs2.io.file.Path
import stryker4jvm.core.model.*
import stryker4jvm.mutator.scala.exception.UnableToBuildPatternMatchException
import stryker4jvm.mutator.scala.extensions.TreeExtensions.{
  CollectFirstExtension,
  FindExtension,
  IsEqualExtension,
  PositionExtension
}
import stryker4jvm.mutator.scala.extensions.mutationtype.*
import stryker4jvm.mutator.scala.model.PlaceableTree
import stryker4jvm.mutator.scala.mutants.MutantsWithId
import stryker4jvm.mutator.scala.mutants.applymutants.ActiveMutationContext

import scala.meta.*
import stryker4jvm.mutator.scala.mutants.language.ScalaAST
import stryker4jvm.mutator.scala.scalatest.LogMatchers
import stryker4jvm.mutator.scala.testutil.{Stryker4sSuite, TestData}

class MutantInstrumenterTest extends Stryker4sSuite with TestData with LogMatchers {

  val path = Path("foo/bar.scala")

  describe("instrumentFile") {
    it("should transform 2 mutations into a match statement with 2 mutated and 1 original") {
      // Arrange
      val source = source"""class Foo { def foo = x >= 15 }"""
      val originalStatement = source.find(q"x >= 15").value
      // val context = SourceContext(source, path)
      val context = new ScalaAST(source = source)
      val mutants = Map(
        PlaceableTree(originalStatement) ->
          toMutations(originalStatement, GreaterThan, q"x > 15", q"x <= 15")
      )
      val sut = new MutantInstrumenter(InstrumenterOptions.testRunner)

      //   // Act
      val mutatedSource = sut.instrumentFile(context, mutants).mutatedSource
      val result = mutatedSource.collectFirst { case t: Term.Match => t }.value

      //   // Assert
      assert(result.expr.isEqual(q"_root_.stryker4s.activeMutation"), result.expr)
      result.cases.map(_.syntax) should (
        contain
          .inOrderOnly(
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
      assert(mutatedSource.isEqual(expected), mutatedSource)
      "Failed to instrument mutants" shouldNot be(loggedAsError)
    }

    it("should place mutants on the correct statement even if the name appears twice") {
      val source = source"""class Foo {
        def foo = {
          val bar = true
          if (bar) 1 else 2
        }
      }"""
      val bars = source.collect { case f @ Term.Name("bar") => f }
      // bars should have length 2
      val originalStatement = bars.last

      // val context = SourceContext(source, path)
      val context = new ScalaAST(source = source)
      val mutants = Map(
        PlaceableTree(originalStatement) -> toMutations(originalStatement, ConditionalTrue, q"true", q"false")
      )
      val sut = new MutantInstrumenter(InstrumenterOptions.testRunner)

      //   // Act
      val mutatedSource = sut.instrumentFile(context, mutants).mutatedSource
      val result = mutatedSource.collectFirst { case t: Term.Match => t }.value

      //   // Assert
      assert(result.expr.isEqual(q"_root_.stryker4s.activeMutation"), result.expr)
      result.cases.map(_.syntax) should (
        contain
          .inOrderOnly(
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
      assert(mutatedSource.isEqual(expected), mutatedSource)
    }

    it("should apply the correct instrumenter options") {
      val source = source"""class Foo { def foo = x >= 15 }"""
      val originalStatement = source.find(q"x >= 15").value
      // val context = SourceContext(source, path)
      val context = new ScalaAST(source = source)
      val mutants = Map(
        PlaceableTree(originalStatement) ->
          toMutations(originalStatement, GreaterThan, q"x > 15", q"x <= 15")
      )
      val sut = new MutantInstrumenter(InstrumenterOptions.sysContext(ActiveMutationContext.envVar))

      //   // Act
      val result = sut.instrumentFile(context, mutants).mutatedSource.collectFirst { case t: Term.Match => t }.value

      //   // Assert
      assert(result.expr.isEqual(q"_root_.scala.sys.env.get(${Lit.String("ACTIVE_MUTATION")})"), result.expr)
      assert(result.cases.head.pat.isEqual(p"Some(${Lit.String("0")})"), result.cases.head.pat.syntax)
      result.cases.map(_.syntax).last shouldBe p"case _ => x >= 15".syntax
    }

    // describe("buildNewSource") {
    it("should log failures correctly") {
      // Arrange
      val source = """class Foo { def foo = true }""".parse[Source].get
      val original = source.find(q"true").value
      // val context = SourceContext(source, path)
      val context = new ScalaAST(source = source)
      val mutants = Map(PlaceableTree(original) -> toMutations(original, True, q"false", q"true"))

      val sut = new MutantInstrumenter(InstrumenterOptions.testRunner) {
        override def buildMatch(cases: NonEmptyVector[Case]) =
          throw new Exception()
      }

      // Act
      an[UnableToBuildPatternMatchException] shouldBe thrownBy(sut.instrumentFile(context, mutants))

      // Assert
      "Failed to instrument mutants in `foo/bar.scala`. Original statement: [true]" shouldBe loggedAsError
      "Failed mutation(s) '0, 1' at Input.String(\"class Foo { def foo = true }\"):1:23" shouldBe loggedAsError
      "This is likely an issue on Stryker4s's end, please enable debug logging and restart Stryker4s." shouldBe loggedAsError
    }

    it("should rethrow Stryker4sExceptions") {
      val source = """class Foo { def foo = true }""".parse[Source].get
      val original = source.find(q"true").value
      // val context = SourceContext(source, path)
      val context = new ScalaAST(source = source)
      val mutants = Map(PlaceableTree(original) -> toMutations(original, True, q"false", q"true"))

      val expectedException = UnableToBuildPatternMatchException(path, new Exception("e"))
      val sut = new MutantInstrumenter(InstrumenterOptions.testRunner) {
        override def defaultCase(placeableTree: PlaceableTree, mutantIds: NonEmptyList[Int]) =
          throw expectedException
      }

      // Act
      the[UnableToBuildPatternMatchException] thrownBy {
        sut.instrumentFile(context, mutants)
      } shouldBe expectedException
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
        new MutantWithId(
          id,
          new MutatedCode(
            replacement,
            new MutantMetaData(
              original.toString(),
              replacement.toString,
              category.mutationName,
              original.pos.toLocation.asCoreElement
            )
          )
        )
      }
  }
}
