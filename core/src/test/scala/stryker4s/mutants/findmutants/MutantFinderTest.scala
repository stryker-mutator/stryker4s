package stryker4s.mutants.findmutants

import java.nio.file.NoSuchFileException

import better.files.File
import stryker4s.config.Config
import stryker4s.extension.FileExtensions._
import stryker4s.extension.TreeExtensions.IsEqualExtension
import stryker4s.scalatest.{FileUtil, LogMatchers}
import stryker4s.testutil.Stryker4sSuite

import scala.meta._
import scala.meta.parsers.ParseException

class MutantFinderTest extends Stryker4sSuite with LogMatchers {
  implicit private val config: Config = Config.default

  private val exampleClassFile = FileUtil.getResource("scalaFiles/ExampleClass.scala")

  describe("parseFile") {
    it("should parse an existing file") {
      val sut = new MutantFinder(new MutantMatcher)
      val file = exampleClassFile

      val result = sut.parseFile(file)

      val expected = """package stryker4s
                       |
                       |class ExampleClass {
                       |  def foo(num: Int) = num == 10
                       |
                       |  def createHugo = Person(22, "Hugo")
                       |}
                       |
                       |final case class Person(age: Int, name: String)
                       |""".stripMargin.parse[Source].get
      assert(result.isEqual(expected))
    }

    it("should throw an exception on a non-parseable file") {
      val sut = new MutantFinder(new MutantMatcher)
      val file = FileUtil.getResource("scalaFiles/nonParseableFile.notScala")

      val expectedException = the[ParseException] thrownBy sut.parseFile(file)

      expectedException.shortMessage should be("expected class or object definition")
    }

    it("should fail on a nonexistent file") {
      val sut = new MutantFinder(new MutantMatcher)
      val noFile = File("this/does/not/exist.scala")

      lazy val result = sut.parseFile(noFile)

      a[NoSuchFileException] should be thrownBy result
    }
  }

  describe("findMutants") {
    it("should return empty list when given source has no possible mutations") {
      val sut = new MutantFinder(new MutantMatcher)
      val source = source"case class Foo(s: String)"

      val result = sut.findMutants(source)._1

      result should be(empty)
    }

    it("should contain a mutant when given source has a possible mutation") {
      val sut = new MutantFinder(new MutantMatcher)
      val source =
        source"""case class Bar(s: String) {
                    def foobar = s == "foobar"
                  }"""

      val result = sut.findMutants(source)._1

      result should have length 2

      val firstMutant = result.head
      assert(firstMutant.original.isEqual(q"=="))
      assert(firstMutant.mutated.isEqual(q"!="))

      val secondMutant = result(1)
      assert(secondMutant.original.isEqual(Lit.String("foobar")))
      assert(secondMutant.mutated.isEqual(Lit.String("")))
    }

    it("should filter out excluded mutants") {
      val conf: Config = config.copy(excludedMutations = Set("LogicalOperator"))
      val sut = new MutantFinder(new MutantMatcher()(conf))(conf)
      val source =
        source"""case class Bar(s: String) {
                    def and(a: Boolean, b: Boolean) = a && b
                  }"""

      val (result, excluded) = sut.findMutants(source)
      excluded shouldBe 1
      result should have length 0
    }

    it("should filter out string mutants inside annotations") {
      val sut = new MutantFinder(new MutantMatcher)
      val source =
        source"""@Annotation("Class Annotation")
                 final case class Bar(
                    @Annotation("Parameter Annotation") s: String = "s") {

                    @Annotation("Function Annotation")
                    def aFunction(@Annotation("Parameter Annotation 2") param: String = "s") = {
                      "aFunction"
                    }

                    @Annotation("Val Annotation") val x = { val l = "x"; l }
                    @Annotation("Var Annotation") var y = { val k = "y"; k }
                  }
                  @Annotation("Object Annotation")
                  object Foo {
                    val value = "value"
                  }
          """

      val (result, excluded) = sut.findMutants(source)
      excluded shouldBe 0
      result should have length 6
    }

    it("should filter out @SuppressWarnings annotated code") {
      val sut = new MutantFinder(new MutantMatcher)

      val source =
        source"""
                 final case class Bar(
                    @SuppressWarnings(Array("stryker4s.mutation.StringLiteral"))
                    s1: String = "filtered",
                    @SuppressWarnings(Array("stryker4s.mutation.StringLiteral"))
                    notFiltered1: Boolean = false) {

                    def aFunction(@SuppressWarnings param: String = "notFiltered2") = {
                      "notFiltered3"
                    }

                    @SuppressWarnings(Array("stryker4s.mutation.StringLiteral"))
                    val x = { val l = "s3"; l }
                  }
                  @SuppressWarnings(Array("stryker4s.mutation.StringLiteral"))
                  object Foo {
                    val value = "s5"
                  }
          """

      val (result, excluded) = sut.findMutants(source)
      excluded shouldBe 3
      result should have length 3
    }
  }

  describe("mutantsInFile") {
    it("should return a FoundMutantsInSource with correct mutants") {
      val sut = new MutantFinder(new MutantMatcher)
      val file = exampleClassFile

      val result = sut.mutantsInFile(file)

      result.source.children should not be empty
      val firstMutant = result.mutants.head
      assert(firstMutant.original.isEqual(q"=="))
      assert(firstMutant.mutated.isEqual(q"!="))

      val secondMutant = result.mutants(1)
      assert(secondMutant.original.isEqual(Lit.String("Hugo")))
      assert(secondMutant.mutated.isEqual(Lit.String("")))
    }
  }

  describe("logging") {
    it("should error log an unfound file") {
      val sut = new MutantFinder(new MutantMatcher)
      val noFile = FileUtil.getResource("scalaFiles/nonParseableFile.notScala")

      a[ParseException] should be thrownBy sut.parseFile(noFile)

      s"Error while parsing file '${noFile.relativePath}', expected class or object definition" should be(loggedAsError)
    }
  }
}
