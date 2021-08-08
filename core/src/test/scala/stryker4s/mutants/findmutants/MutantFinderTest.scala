package stryker4s.mutants.findmutants

import fs2.io.file.Path
import stryker4s.config.Config
import stryker4s.extension.FileExtensions._
import stryker4s.extension.TreeExtensions.IsEqualExtension
import stryker4s.log.Logger
import stryker4s.scalatest.{FileUtil, LogMatchers}
import stryker4s.testutil.Stryker4sSuite

import java.nio.file.NoSuchFileException
import scala.meta._
import scala.meta.parsers.ParseException

class MutantFinderTest extends Stryker4sSuite with LogMatchers {
  private val exampleClassFile = FileUtil.getResource("scalaFiles/ExampleClass.scala")

  describe("parseFile") {
    implicit val config: Config = Config.default
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
      assert(result.isEqual(expected), result)
    }

    it("should throw an exception on a non-parseable file") {
      val sut = new MutantFinder(new MutantMatcher)
      val file = FileUtil.getResource("scalaFiles/nonParseableFile.notScala")

      val expectedException = the[ParseException] thrownBy sut.parseFile(file)

      expectedException.shortMessage should be("expected class or object definition identifier")
    }

    it("should fail on a nonexistent file") {
      val sut = new MutantFinder(new MutantMatcher)
      val noFile = Path("this/does/not/exist.scala")

      lazy val result = sut.parseFile(noFile)

      a[NoSuchFileException] should be thrownBy result
    }

    it("should parse a scala-3 file") {
      import scala.meta.dialects.Scala3

      val scala3DialectConfig = config.copy(scalaDialect = Scala3)
      val sut = new MutantFinder(new MutantMatcher)(scala3DialectConfig, implicitly[Logger])
      val file = FileUtil.getResource("scalaFiles/scala3File.scala")

      noException shouldBe thrownBy(sut.parseFile(file))
    }
  }

  describe("findMutants") {
    it("should return empty list when given source has no possible mutations") {
      implicit val config: Config = Config.default

      val sut = new MutantFinder(new MutantMatcher)
      val source = source"case class Foo(s: String)"

      val result = sut.findMutants(source)._1

      result should be(empty)
    }

    it("should contain a mutant when given source has a possible mutation") {
      implicit val config: Config = Config.default

      val sut = new MutantFinder(new MutantMatcher)
      val source =
        source"""case class Bar(s: String) {
                    def foobar = s == "foobar"
                  }"""

      val result = sut.findMutants(source)._1

      result should have length 2

      val firstMutant = result.head
      assert(firstMutant.original.isEqual(q"=="), firstMutant.original)
      assert(firstMutant.mutated.isEqual(q"!="), firstMutant.mutated)

      val secondMutant = result(1)
      assert(secondMutant.original.isEqual(Lit.String("foobar")), secondMutant.original)
      assert(secondMutant.mutated.isEqual(Lit.String("")), secondMutant.mutated)
    }

    it("should filter out excluded mutants") {
      implicit val conf: Config = Config.default.copy(excludedMutations = Set("LogicalOperator"))
      val sut = new MutantFinder(new MutantMatcher())
      val source =
        source"""case class Bar(s: String) {
                    def and(a: Boolean, b: Boolean) = a && b
                  }"""

      val (result, excluded) = sut.findMutants(source)
      excluded shouldBe 1
      result should have length 0
    }

    it("should filter out string mutants inside annotations") {
      implicit val config: Config = Config.default
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
      implicit val config: Config = Config.default
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

    it("should log unparsable regular expressions") {
      implicit val config: Config = Config.default

      val sut = new MutantFinder(new MutantMatcher)
      val regex = Lit.String("[[]]")
      val source =
        source"""case class Bar() {
                    def foobar = new Regex($regex)
                  }"""

      val result = sut.findMutants(source)

      // 1 empty-string found
      val mutant = result._1.loneElement
      assert(mutant.original.isEqual(regex), mutant.original)
      assert(mutant.mutated.isEqual(Lit.String("")), mutant.mutated)
      // 0 excluded
      result._2 shouldBe 0

      "[RegexMutator]: The Regex parser of weapon-regex couldn't parse this regex pattern: '[[]]'. Please report this issue at https://github.com/stryker-mutator/weapon-regex/issues. Inner error:" shouldBe loggedAsError
    }
  }

  describe("mutantsInFile") {
    implicit val config: Config = Config.default

    it("should return a FoundMutantsInSource with correct mutants") {
      val sut = new MutantFinder(new MutantMatcher)
      val file = exampleClassFile

      val result = sut.mutantsInFile(file)

      result.source.children should not be empty
      val firstMutant = result.mutants.head
      assert(firstMutant.original.isEqual(q"=="), firstMutant.original)
      assert(firstMutant.mutated.isEqual(q"!="), firstMutant.mutated)

      val secondMutant = result.mutants(1)
      assert(secondMutant.original.isEqual(Lit.String("Hugo")), secondMutant.original)
      assert(secondMutant.mutated.isEqual(Lit.String("")), secondMutant.mutated)
    }
  }

  describe("logging") {
    it("should error log an unfound file") {
      implicit val config: Config = Config.default

      val sut = new MutantFinder(new MutantMatcher)
      val noFile = FileUtil.getResource("scalaFiles/nonParseableFile.notScala")

      a[ParseException] should be thrownBy sut.parseFile(noFile)

      s"Error while parsing file '${noFile.relativePath}', expected class or object definition" should be(loggedAsError)
    }
  }
}
