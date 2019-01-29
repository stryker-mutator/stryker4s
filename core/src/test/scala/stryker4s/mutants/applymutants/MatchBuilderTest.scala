package stryker4s.mutants.applymutants

import stryker4s.extension.TreeExtensions._
import stryker4s.extension.mutationtype._
import stryker4s.model.{Mutant, SourceTransformations, TransformedMutants}
import stryker4s.scalatest.TreeEquality
import stryker4s.testutil.Stryker4sSuite

import scala.language.postfixOps
import scala.meta._
import scala.meta.contrib._

class MatchBuilderTest extends Stryker4sSuite with TreeEquality {
  private val activeMutationExpr: Term.Apply = {
    val activeMutation = Lit.String("ACTIVE_MUTATION")
    q"sys.props.get($activeMutation).orElse(sys.env.get($activeMutation))"
  }

  describe("buildMatch") {
    it("should transform 2 mutations into match statement with 2 mutated and 1 original") {
      // Arrange
      val ids = Iterator.from(0)
      val originalStatement = q"x >= 15"
      val mutants = List(q"x > 15", q"x <= 15")
        .map(Mutant(ids.next(), originalStatement, _, GreaterThan))
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
      implicit val ids: Iterator[Int] = Iterator.from(0)
      val source = "class Foo { def bar: Boolean = 15 > 14 }".parse[Source].get

      val transformed = toTransformed(source, GreaterThan, q">", q"<", q"==", q">=")
      val transStatements =
        SourceTransformations(source, List(transformed))
      val sut = new MatchBuilder

      // Act
      val result = sut.buildNewSource(transStatements)

      // Assert
      val expected =
        """class Foo {
          |  def bar: Boolean = sys.props.get("ACTIVE_MUTATION").orElse(sys.env.get("ACTIVE_MUTATION")) match {
          |    case Some("0") =>
          |      15 < 14
          |    case Some("1") =>
          |      15 == 14
          |    case Some("2") =>
          |      15 >= 14
          |    case _ =>
          |      15 > 14
          |  }
          |}""".stripMargin.parse[Source].get
      result should equal(expected)
    }

    it("should build a tree with multiple cases out of multiple transformedStatements") {
      // Arrange
      implicit val ids: Iterator[Int] = Iterator.from(0)
      val source = "class Foo { def bar: Boolean = 15 > 14 && 14 >= 13 }".parse[Source].get
      val firstTrans = toTransformed(source, GreaterThan, q">", q"<", q"==", q">=")
      val secondTrans = toTransformed(source, GreaterThanEqualTo, q">=", q">", q"==", q"<")

      val transformedStatements = SourceTransformations(source, List(firstTrans, secondTrans))
      val sut = new MatchBuilder

      // Act
      val result = sut.buildNewSource(transformedStatements)

      // Assert
      val expected =
        """class Foo {
          |  def bar: Boolean = sys.props.get("ACTIVE_MUTATION").orElse(sys.env.get("ACTIVE_MUTATION")) match {
          |    case Some("0") =>
          |      15 < 14 && 14 >= 13
          |    case Some("1") =>
          |      15 == 14 && 14 >= 13
          |    case Some("2") =>
          |      15 >= 14 && 14 >= 13
          |    case Some("3") =>
          |      15 > 14 && 14 > 13
          |    case Some("4") =>
          |      15 > 14 && 14 == 13
          |    case Some("5") =>
          |      15 > 14 && 14 < 13
          |    case _ =>
          |      15 > 14 && 14 >= 13
          |  }
          |}""".stripMargin.parse[Source].get
      result should equal(expected)
    }

    it("should build a new tree out of a single statement with 3 mutants") {
      // Arrange
      implicit val ids: Iterator[Int] = Iterator.from(0)
      val source = """class Foo { def foo = "foo" == "" }""".parse[Source].get

      val firstTransformed = toTransformed(source, EmptyString, Lit.String("foo"), Lit.String(""))
      val secondTransformed = toTransformed(source, NotEqualTo, q"==", q"!=")
      val thirdTransformed =
        toTransformed(source, StrykerWasHereString, Lit.String(""), Lit.String("Stryker was here!"))

      val transformedStatements =
        SourceTransformations(source, List(firstTransformed, secondTransformed, thirdTransformed))
      val sut = new MatchBuilder

      // Act
      val result = sut.buildNewSource(transformedStatements)

      // Assert
      val expected =
        """class Foo {
          |  def foo = sys.props.get("ACTIVE_MUTATION").orElse(sys.env.get("ACTIVE_MUTATION")) match {
          |    case Some("0") =>
          |      "" == ""
          |    case Some("1") =>
          |      "foo" != ""
          |    case Some("2") =>
          |      "foo" == "Stryker was here!"
          |    case _ =>
          |      "foo" == ""
          |  }
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

  /** Helper method to create a [[stryker4s.model.TransformedMutants]] out of a statement and it's mutants
    */
  private def toTransformed(source: Source, mutation: Mutation[_ <: Tree], origStatement: Term, mutants: Term*)(
      implicit ids: Iterator[Int]): TransformedMutants = {
    val topStatement = source.find(origStatement).value.topStatement()
    val mutant = mutants
      .map(m => topStatement transformOnce { case orig if orig.isEqual(origStatement) => m } get)
      .map(m => Mutant(ids.next(), topStatement, m.asInstanceOf[Term], mutation))
      .toList

    TransformedMutants(topStatement, mutant)
  }
}
