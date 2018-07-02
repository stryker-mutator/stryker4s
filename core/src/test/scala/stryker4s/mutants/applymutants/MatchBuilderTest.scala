package stryker4s.mutants.applymutants

import stryker4s.Stryker4sSuite
import stryker4s.extensions.TreeExtensions._
import stryker4s.model.{Mutant, SourceTransformations, TransformedMutants}
import stryker4s.scalatest.TreeEquality

import scala.meta._

class MatchBuilderTest extends Stryker4sSuite with TreeEquality {
  private val activeMutationExpr: Term.Apply = {
    val activeMutation = Lit.String("ACTIVE_MUTATION")
    q"sys.env.get($activeMutation)"
  }

  describe("buildMatch") {
    it("should transform 2 mutations into match statement with 2 mutated and 1 original") {
      // Arrange
      val ids = Iterator.from(0)
      val originalStatement = q"x >= 15"
      val mutants = List(q"x > 15", q"x <= 15")
        .map(Mutant(ids.next(), originalStatement, _))
      val sut = new MatchBuilder

      // Act
      val result = sut.buildMatch(TransformedMutants(originalStatement, mutants))

      // Assert
      result.expr should equal(activeMutationExpr)
      val someZero = someOf(0)
      val someOne = someOf(1)
      result.cases should contain inOrderOnly (p"case $someZero => x > 15", p"case $someOne => x <= 15", p"case _ => x >= 15")
    }
  }

  describe("buildNewSource") {
    it("should build a new tree with a case match in place of the 15 > 14 statement") {
      // Arrange
      val ids = Iterator.from(0)
      val source = "class Foo { def bar: Boolean = 15 > 14 }".parse[Source].get
      val origStatement = source.find(q">").value.topStatement()
      val mutants = List(q"15 < 14", q"15 == 14")
        .map(Mutant(ids.next(), origStatement, _))
      val transStatements =
        SourceTransformations(source, List(TransformedMutants(origStatement, mutants)))
      val sut = new MatchBuilder

      // Act
      val result = sut.buildNewSource(transStatements)

      // Assert
      val expected =
        """class Foo {
          |  def bar: Boolean = sys.env.get("ACTIVE_MUTATION") match {
          |    case Some("0") =>
          |      15 < 14
          |    case Some("1") =>
          |      15 == 14
          |    case _ =>
          |      15 > 14
          |  }
          |}""".stripMargin.parse[Source].get
      result should equal(expected)
    }

    it("should build a tree with multiple cases out of multiple transformedStatements") {
      // Arrange
      val ids = Iterator.from(0)
      val source = "class Foo { def bar: Boolean = 15 > 14 && 14 >= 13 }".parse[Source].get

      val firstOrig = source.find(q">").value.topStatement()
      val firstMutants = List(q"15 < 14", q"15 == 14")
        .map(Mutant(ids.next(), firstOrig, _))
      val firstTrans = TransformedMutants(firstOrig, firstMutants)

      val secondOrig = source.find(q">=").value.topStatement()
      val secondMutants = List(q"14 > 13", q"14 == 13")
        .map(Mutant(ids.next(), firstOrig, _))
      val secondTrans = TransformedMutants(secondOrig, secondMutants)

      val transformedStatements =
        SourceTransformations(source, List(firstTrans, secondTrans))
      val sut = new MatchBuilder

      // Act
      val result = sut.buildNewSource(transformedStatements)

      // Assert
      val expected =
        """class Foo {
          |  def bar: Boolean = (sys.env.get("ACTIVE_MUTATION") match {
          |    case Some("0") =>
          |      15 < 14
          |    case Some("1") =>
          |      15 == 14
          |    case _ =>
          |      15 > 14
          |  }) && (sys.env.get("ACTIVE_MUTATION") match {
          |    case Some("2") =>
          |      14 > 13
          |    case Some("3") =>
          |      14 == 13
          |    case _ =>
          |      14 >= 13
          |  })
          |}""".stripMargin.parse[Source].get
      result should equal(expected)
    }
  }

  /** Little helper method to get a Some(*string*) AST out of an Int
    */
  private def someOf(number: Int): Pat.Extract = {
    val stringTerm = Lit.String(number.toString)
    p"Some($stringTerm)"
  }

}
