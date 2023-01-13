package stryker4jvm.mutator.scala

import stryker4jvm.mutator.scala.testutil.Stryker4sSuite
import fs2.io.file.Path
import scala.meta.*

import stryker4jvm.mutator.scala.extensions.TreeExtensions.{
  CollectFirstExtension,
  FindExtension,
  IsEqualExtension,
  PositionExtension
}
import stryker4jvm.mutator.scala.extensions.Stryker4jvmCoreConversions.LocationExtension
import scala.collection.JavaConverters.*

import stryker4jvm.mutator.scala.extensions.mutationtype.*
import cats.data.{NonEmptyList, NonEmptyVector}

import stryker4jvm.core.model.MutantWithId
import stryker4jvm.core.model.MutatedCode
import stryker4jvm.core.model.MutantMetaData
import java.util as ju

class ScalaInstrumenterTest extends Stryker4sSuite {
  val path = Path("foo/bar.scala")

  describe("instrumentFile") {
    it("should transform 2 mutations into a match statement with 2 mutated and 1 original") {
      // Arrange
      val source = source"""class Foo { def foo = x >= 15 }"""
      val originalStatement = source.find(q"x >= 15").value

      val src = new ScalaAST(value = source)
      val mutants = Map(
        new ScalaAST(value = originalStatement) ->
          toMutations(originalStatement, GreaterThan, q"x > 15", q"x <= 15")
      ).asJava

      val sut = new ScalaInstrumenter(ScalaInstrumenterOptions.testRunner)

      //   // Act
      val mutatedSource = sut.instrument(src, mutants).value
      val result = mutatedSource.collectFirst { case t: Term.Match => t }.value

      //   // Assert
      assert(result.expr.isEqual(q"_root_.stryker4jvm.activeMutation"), result.expr)
      result.cases.map(_.syntax) should (
        contain
          .inOrderOnly(
            p"case 0 => x > 15".syntax,
            p"case 1 => x <= 15".syntax,
            p"case _ if _root_.stryker4jvm.coverage.coverMutant(0, 1) => x >= 15".syntax
          )
      )
      val expected = source"""class Foo {
                   def foo = _root_.stryker4jvm.activeMutation match {
                     case 0 =>
                       x > 15
                     case 1 =>
                       x <= 15
                     case _ if _root_.stryker4jvm.coverage.coverMutant(0, 1) =>
                       x >= 15
                   }
                 }"""
      assert(mutatedSource.isEqual(expected), mutatedSource)
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
      val context = new ScalaAST(value = source)
      val mutants = Map(
        new ScalaAST(value = originalStatement) -> toMutations(originalStatement, ConditionalTrue, q"true", q"false")
      ).asJava
      val sut = new ScalaInstrumenter(ScalaInstrumenterOptions.testRunner)

      //   // Act
      val mutatedSource = sut.instrument(context, mutants).value
      val result = mutatedSource.collectFirst { case t: Term.Match => t }.value

      //   // Assert
      assert(result.expr.isEqual(q"_root_.stryker4jvm.activeMutation"), result.expr)
      result.cases.map(_.syntax) should (
        contain
          .inOrderOnly(
            p"case 0 => true".syntax,
            p"case 1 => false".syntax,
            p"case _ if _root_.stryker4jvm.coverage.coverMutant(0, 1) => bar".syntax
          )
      )
      val expected = source"""class Foo {
          def foo = {
            val bar = true
            if (
              _root_.stryker4jvm.activeMutation match {
                case 0 =>
                  true
                case 1 =>
                  false
                case _ if _root_.stryker4jvm.coverage.coverMutant(0, 1) =>
                  bar
              }
            ) 1
            else 2
          }
        }"""
      assert(mutatedSource.isEqual(expected), mutatedSource)
    }

  }

  def toMutations[T <: Tree](
      original: Term,
      category: Mutation[T],
      firstReplacement: Term,
      replacements: Term*
  ): ju.List[MutantWithId[ScalaAST]] = {
    val vec = Vector(firstReplacement) ++ replacements.toVector

    vec.zipWithIndex.map { case (replacement, id) =>
      new MutantWithId(
        id,
        new MutatedCode(
          new ScalaAST(value = replacement),
          new MutantMetaData(
            original.toString(),
            replacement.toString,
            category.mutationName,
            original.pos.toLocation.asCoreElement
          )
        )
      )
    }.asJava

  }
}
