package stryker4s.mutants.applymutants

import stryker4s.extension.TreeExtensions._
import stryker4s.extension.exception.UnableToBuildPatternMatchException
import stryker4s.extension.mutationtype._
import stryker4s.model.{Mutant, SourceTransformations, TransformedMutants}
import stryker4s.scalatest.{LogMatchers, TreeEquality}
import stryker4s.testutil.SyncStryker4sSuite

import scala.meta._

class MatchBuilderTest extends SyncStryker4sSuite with TreeEquality with LogMatchers {
  private val activeMutationString = Lit.String("ACTIVE_MUTATION")
  private val activeMutationPropsExpr: Term.Apply = q"_root_.scala.sys.props.get($activeMutationString)"

  describe("buildMatch") {
    it("should transform 2 mutations into match statement with 2 mutated and 1 original") {
      // Arrange
      val ids = Iterator.from(0)
      val originalStatement = q"x >= 15"
      val mutants = List(q"x > 15", q"x <= 15")
        .map(Mutant(ids.next(), originalStatement, _, GreaterThan))
      val sut = new MatchBuilder(ActiveMutationContext.sysProps)

      // Act
      val result = sut.buildMatch(TransformedMutants(originalStatement, mutants))

      // Assert
      result.expr should equal(activeMutationPropsExpr)
      val someZero = someOf(0)
      val someOne = someOf(1)
      result.cases should contain inOrderOnly (p"case $someZero => x > 15", p"case $someOne => x <= 15", p"case _ => x >= 15")
    }
  }

  describe("buildNewSource") {
    it("should log failures correctly") {
      // Arrange
      implicit val ids: Iterator[Int] = Iterator.from(0)
      val source = """class Foo { def foo = true }""".parse[Source].get

      val firstTransformed = toTransformed(source, EmptyString, Lit.Boolean(true), Lit.Boolean(false))

      val transformedStatements = SourceTransformations(source, List(firstTransformed))
      val sut = new MatchBuilder(ActiveMutationContext.sysProps) {
        override def buildMatch(transformedMutant: TransformedMutants): Term.Match =
          throw new Exception()
      }

      // Act
      an[UnableToBuildPatternMatchException] shouldBe thrownBy(sut.buildNewSource(transformedStatements))

      // Assert
      "Failed to construct pattern match: original statement [true]" shouldBe loggedAsError
      "Failed mutation(s) Mutant(0,true,false,EmptyString)." shouldBe loggedAsError
      "at Input.String(\"class Foo { def foo = true }\"):1:23" shouldBe loggedAsError
      "This is likely an issue on Stryker4s's end, please enable debug logging and restart Stryker4s." shouldBe loggedAsError

      "Please open an issue on github: https://github.com/stryker-mutator/stryker4s/issues/new" shouldBe loggedAsDebug
      "Please be so kind to copy the stacktrace into the issue" shouldBe loggedAsDebug
    }

    it("should log mutations that couldn't be applied") {
      // Arrange
      val source = """class Foo { def bar: Boolean = 15 > 14 }""".parse[Source].get

      val failedMutants = List(
        Mutant(0, q"foo", q"bar", GreaterThan),
        Mutant(1, q"baz", q"qux", GreaterThan)
      )
      val successfulMutants = List(
        Mutant(2, q">", q"15 < 14", GreaterThan),
        Mutant(3, q">", q"15 <= 14", GreaterThan)
      )
      val transformed = TransformedMutants(q"14 < 15", failedMutants)
      val successfulTransformed = TransformedMutants(source.find(q"15 > 14").value, successfulMutants)
      val transStatements = SourceTransformations(source, List(transformed, successfulTransformed))
      val sut = new MatchBuilder(ActiveMutationContext.sysProps)

      // Act
      val result = sut.buildNewSource(transStatements)

      // Assert
      val expected = source"""class Foo {
        def bar: Boolean = _root_.scala.sys.props.get("ACTIVE_MUTATION") match {
              case Some("2") =>
                15 < 14
              case Some("3") =>
                15 <= 14
              case _ =>
                15 > 14
            }
      }"""
      result should equal(expected)
      s"Failed to add mutation(s) 0, 1 to new mutated code" shouldBe loggedAsWarning
      s"The code that failed to mutate was: [14 < 15] at Input.None:0:0" shouldBe loggedAsWarning
      "This mutation will likely show up as Survived" shouldBe loggedAsWarning
      "Please open an issue on github with sample code of the mutation that failed: https://github.com/stryker-mutator/stryker4s/issues/new" shouldBe loggedAsWarning
    }

    it("should build a new tree with a case match in place of the 15 > 14 statement") {
      // Arrange
      implicit val ids: Iterator[Int] = Iterator.from(0)
      val source = "class Foo { def bar: Boolean = 15 > 14 }".parse[Source].get

      val transformed = toTransformed(source, GreaterThan, q">", q"<", q"==", q">=")
      val transStatements =
        SourceTransformations(source, List(transformed))
      val sut = new MatchBuilder(ActiveMutationContext.sysProps)

      // Act
      val result = sut.buildNewSource(transStatements)

      // Assert
      val expected =
        """class Foo {
          |  def bar: Boolean = _root_.scala.sys.props.get("ACTIVE_MUTATION") match {
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
      val sut = new MatchBuilder(ActiveMutationContext.sysProps)

      // Act
      val result = sut.buildNewSource(transformedStatements)

      // Assert
      val expected =
        """class Foo {
          |  def bar: Boolean = _root_.scala.sys.props.get("ACTIVE_MUTATION") match {
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

      val firstTransformed = toTransformed(source, NotEqualTo, q"==", q"!=")
      val secondTransformed = toTransformed(source, EmptyString, Lit.String("foo"), Lit.String(""))
      val thirdTransformed =
        toTransformed(source, StrykerWasHereString, Lit.String(""), Lit.String("Stryker was here!"))

      val transformedStatements =
        SourceTransformations(source, List(firstTransformed, secondTransformed, thirdTransformed))
      val sut = new MatchBuilder(ActiveMutationContext.sysProps)

      // Act
      val result = sut.buildNewSource(transformedStatements)

      // Assert
      val expected =
        source"""class Foo {
            def foo = _root_.scala.sys.props.get("ACTIVE_MUTATION") match {
              case Some("0") =>
                "foo" != ""
              case Some("1") =>
                "" == ""
              case Some("2") =>
                "foo" == "Stryker was here!"
              case _ =>
                "foo" == ""
            }
          }"""
      result should equal(expected)
    }

    it("should build when the topstatement is also a mutation") {
      // Arrange
      implicit val ids: Iterator[Int] = Iterator.from(0)
      val source = source"""class Foo(list: Seq[String]) {
                              def foo =
                                list.nonEmpty match {
                                  case true => "nonEmpty"
                                  case _    => otherValue
                                }
                            }
                            """

      val firstTransformed = toTransformed(source, IsEmpty, q"nonEmpty", q"isEmpty")
      val secondTransformed = toTransformed(source, False, q"true", q"false")
      val thirdTransformed =
        toTransformed(source, EmptyString, Lit.String("nonEmpty"), Lit.String(""))
      val transformedStatements =
        SourceTransformations(source, List(firstTransformed, secondTransformed, thirdTransformed))
      val sut = new MatchBuilder(ActiveMutationContext.sysProps)

      // Act
      val result = sut.buildNewSource(transformedStatements)

      // Assert
      val expected = source"""class Foo(list: Seq[String]) {
                                def foo =
                                  _root_.scala.sys.props.get("ACTIVE_MUTATION") match {
                                    case Some("0") =>
                                      list.isEmpty match {
                                        case true => "nonEmpty"
                                        case _    => otherValue
                                      }
                                    case Some("1") =>
                                      list.nonEmpty match {
                                        case false => "nonEmpty"
                                        case _     => otherValue
                                      }
                                    case Some("2") =>
                                      list.nonEmpty match {
                                        case true => ""
                                        case _    => otherValue
                                      }
                                    case _ =>
                                      list.nonEmpty match {
                                        case true => "nonEmpty"
                                        case _    => otherValue
                                      }
                                  }
                              }
                              """
      result should equal(expected)
    }

    it("should include all mutants in a try-catch-finally") {
// Arrange
      implicit val ids: Iterator[Int] = Iterator.from(0)
      val source = source"""class Foo() {
                              def foo =
                                try {
                                  runAndContinue("task.run")
                                } catch {
                                  case _ => logger.error("Error during run", e)
                                } finally {
                                  logger.info("Done")
                                }
                            }"""

      val firstTransformed = toTransformed(source, EmptyString, Lit.String("task.run"), Lit.String(""))
      val secondTransformed = toTransformed(source, EmptyString, Lit.String("Error during run"), Lit.String(""))
      val thirdTransformed = toTransformed(source, EmptyString, Lit.String("Done"), Lit.String(""))
      val transformedStatements =
        SourceTransformations(source, List(firstTransformed, secondTransformed, thirdTransformed))
      val sut = new MatchBuilder(ActiveMutationContext.sysProps)

      // Act
      val result = sut.buildNewSource(transformedStatements)

      // Assert
      val expected = source"""class Foo() {
                                def foo = try {
                                  _root_.scala.sys.props.get("ACTIVE_MUTATION") match {
                                    case Some("0") =>
                                      runAndContinue("")
                                    case _ =>
                                      runAndContinue("task.run")
                                  }
                                } catch {
                                  case _ =>
                                    _root_.scala.sys.props.get("ACTIVE_MUTATION") match {
                                      case Some("1") =>
                                        logger.error("", e)
                                      case _ =>
                                        logger.error("Error during run", e)
                                    }
                                } finally {
                                  _root_.scala.sys.props.get("ACTIVE_MUTATION") match {
                                    case Some("2") =>
                                      logger.info("")
                                    case _ =>
                                      logger.info("Done")
                                  }
                                }
                              }"""
      result should equal(expected)
    }
  }

  describe("mutationActivation") {
    it("should build a pattern match with sys.props if sysProps is given") {
      val sut = new MatchBuilder(ActiveMutationContext.sysProps)
      implicit val ids: Iterator[Int] = Iterator.from(0)
      val source = """class Foo { def foo = "foo" == "" }""".parse[Source].get
      val transformed = toTransformed(source, EmptyString, Lit.String("foo"), Lit.String(""))

      val result = sut.buildMatch(transformed)

      result.expr should equal(q"_root_.scala.sys.props.get($activeMutationString)")
    }

    it("should build a pattern match with sys.env if envVar is given") {
      val sut = new MatchBuilder(ActiveMutationContext.envVar)
      implicit val ids: Iterator[Int] = Iterator.from(0)
      val source = """class Foo { def foo = "foo" == "" }""".parse[Source].get
      val transformed = toTransformed(source, EmptyString, Lit.String("foo"), Lit.String(""))

      val result = sut.buildMatch(transformed)

      result.expr should equal(q"_root_.scala.sys.env.get($activeMutationString)")
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
  private def toTransformed(source: Source, mutation: Mutation[_ <: Tree], origStatement: Term, mutants: Term*)(implicit
      ids: Iterator[Int]
  ): TransformedMutants = {
    val topStatement = source.find(origStatement).value.topStatement()
    val mutant = mutants
      .map(m => topStatement.transformOnce { case orig if orig.isEqual(origStatement) => m }.get)
      .map(m => Mutant(ids.next(), topStatement, m.asInstanceOf[Term], mutation))
      .toList

    TransformedMutants(topStatement, mutant)
  }
}
