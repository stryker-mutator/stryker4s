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

      val src = new ScalaAST(source = source)
      val mutants = Map(
        new ScalaAST(tree = originalStatement) ->
          toMutations(originalStatement, GreaterThan, q"x > 15", q"x <= 15")
      ).asJava

      val sut = new ScalaInstrumenter()

      //   // Act
      val mutatedSource = sut.instrument(src, mutants).tree
      val result = mutatedSource.collectFirst { case t: Term.Match => t }.value

      //   // Assert
      println(result.expr)
      // assert(result.expr.isEqual(q"_root_.stryker4s.activeMutation"), result.expr)

      println(originalStatement)

    }
  }

  def toMutations[T <: Tree](
      original: Term,
      category: Mutation[T],
      firstReplacement: Term,
      replacements: Term*
  ): ju.List[MutantWithId[ScalaAST]] = {

    val vec = Vector(firstReplacement) ++ Vector.from(replacements)

    vec.zipWithIndex.map { case (replacement, id) =>
      new MutantWithId(
        id,
        new MutatedCode(
          new ScalaAST(term = replacement),
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
