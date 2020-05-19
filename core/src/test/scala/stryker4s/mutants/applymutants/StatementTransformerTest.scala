package stryker4s.mutants.applymutants

import stryker4s.extension.ImplicitMutationConversion.mutationToTree
import stryker4s.extension.TreeExtensions._
import stryker4s.extension.mutationtype._
import stryker4s.model.Mutant
import stryker4s.scalatest.TreeEquality
import stryker4s.testutil.SyncStryker4sSuite

import scala.meta._

class StatementTransformerTest extends SyncStryker4sSuite with TreeEquality {
  val sut = new StatementTransformer

  describe("transformStatement") {
    it("should return a single new statement on single FoundMutant") {
      val originalTopTree = q"val x: Boolean = 15 >= 5"
      val originalTree = originalTopTree.find(q">=").value
      val topStatement = originalTree.topStatement()

      val result = sut.transformStatement(topStatement, originalTree, GreaterThan)

      result should equal(q"15 > 5")
    }

    it("should return list of transformed statements on multiple found mutants") {
      val originalTopTree = q"val x: Boolean = 15 >= 5"
      val originalTree = originalTopTree.find(q">=").value
      val topStatement = originalTree.topStatement()

      val result = sut.transformStatement(topStatement, originalTree, EqualTo)

      result should equal(q"15 == 5")
    }

    it("should mutate a more complex tree statement with two similar statements") {
      val tree =
        q"""def foo(list: List[Int], otherList: List[Int]) = {
        val firstResult = list
          .filter(_ % 2 == 0)
          .map(_ * 5)
          .reverse
        val secondResult = otherList
          .filter(_ >= 5)
          .map(_ * 5)
          .drop(5)
        (firstResult, secondResult)
      }"""
      // Second '_ * 5' instead of first one
      val subTree = tree.collect({ case t @ Term.Name("*") => t }).last
      val topStatement = subTree.topStatement()

      val result = sut.transformStatement(topStatement, subTree, q"/")

      result should equal(q"""otherList
          .filter(_ >= 5)
          .map(_ / 5)
          .drop(5)""")
    }
  }

  describe("transformFoundMutant") {
    it("should give a list of transformed statements when multiple mutations are given") {
      // Arrange
      val originalTopTree = q"val x: Boolean = 15 >= 5"
      val originalTree = originalTopTree.find(q">=").value
      val mutants = List(EqualTo, GreaterThan, LesserThanEqualTo)
        .map(Mutant(0, originalTree, _, GreaterThanEqualTo))

      // Act
      val transformedMutant = sut.transformMutant(originalTree, mutants)

      // Assert
      val topStatement = transformedMutant.originalStatement
      val transformedTrees = transformedMutant.mutantStatements
      val mutatedResult = transformedTrees.map(_.mutated)
      topStatement should equal(q"15 >= 5")
      mutatedResult should contain only (q"15 == 5", q"15 > 5", q"15 <= 5")
    }
  }

  describe("transformFoundMutants") {
    it("should transform a single found mutant") {
      // Arrange
      val source = "object Foo { def bar: Boolean = 15 >= 4 }".parse[Source].get
      val origTree = source.find(q">=").value
      val mutants = List(EqualTo, GreaterThan, LesserThanEqualTo)
        .map(Mutant(0, origTree, _, GreaterThanEqualTo))

      // Act
      val result = sut.transformSource(source, mutants)

      // Assert
      result.source should be theSameInstanceAs source
      val le = result.transformedStatements.loneElement
      le.originalStatement should equal(q"15 >= 4")
      le.mutantStatements.map(_.mutated) should contain only (q"15 == 4", q"15 > 4", q"15 <= 4")
    }
  }

  it("should transform multiple found mutants into one TransformedStatements") {
    // Arrange
    val source = "object Foo { def bar: Boolean = 15 >= 4 && 14 < 20 }".parse[Source].get

    val firstOrigTree = source.find(q">=").value
    val firstMutants: Seq[Mutant] = List(EqualTo, GreaterThan, LesserThanEqualTo)
      .map(Mutant(0, firstOrigTree, _, GreaterThanEqualTo))

    val secOrigTree = source.find(q"<").value
    val secondMutants: Seq[Mutant] = List(LesserThanEqualTo, GreaterThan, EqualTo)
      .map(Mutant(0, secOrigTree, _, GreaterThanEqualTo))

    val statements = firstMutants ++ secondMutants

    // Act
    val result = sut.transformSource(source, statements)

    // Assert
    result.source should be theSameInstanceAs source

    result.transformedStatements should have size 2 // Order is unknown
  }
}
