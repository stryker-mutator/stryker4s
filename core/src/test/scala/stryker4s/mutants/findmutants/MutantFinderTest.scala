package stryker4s.mutants.findmutants

import java.nio.file.NoSuchFileException

import better.files.File
import stryker4s.Stryker4sSuite
import stryker4s.config.Config
import stryker4s.extensions.FileExtensions._
import stryker4s.mutants.Exclusions
import stryker4s.scalatest.{FileUtil, LogMatchers, TreeEquality}

import scala.meta._
import scala.meta.parsers.ParseException

class MutantFinderTest extends Stryker4sSuite with TreeEquality with LogMatchers {

  private implicit val config: Config = Config()

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
                       |case class Person(age: Int, name: String)
                       |""".stripMargin.parse[Source].get
      result should equal(expected)
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
      firstMutant.original should equal(q"==")
      firstMutant.mutated should equal(q"!=")

      val secondMutant = result(1)
      secondMutant.original should equal(Lit.String("foobar"))
      secondMutant.mutated should equal(Lit.String(""))
    }

    it("should filter out excluded mutants") {
      val conf: Config = config.copy(excludedMutations = Exclusions(Set("LogicalOperator")))
      val sut = new MutantFinder(new MutantMatcher()(conf))(conf)
      val source =
        source"""case class Bar(s: String) {
                    def and(a: Boolean, b: Boolean) = a && b
                  }"""

      val result = sut.findMutants(source)._1
      val excluded = sut.findMutants(source)._2
      excluded shouldBe 1
      result should have length 0
    }
  }

  describe("mutantsInFile") {
    it("should return a FoundMutantsInSource with correct mutants") {
      val sut = new MutantFinder(new MutantMatcher)
      val file = exampleClassFile

      val result = sut.mutantsInFile(file)

      result.source.children should not be empty
      val firstMutant = result.mutants.head
      firstMutant.original should equal(q"==")
      firstMutant.mutated should equal(q"!=")

      val secondMutant = result.mutants(1)
      secondMutant.original should equal(Lit.String("Hugo"))
      secondMutant.mutated should equal(Lit.String(""))
    }

  }

  describe("logging") {
    it("should error log an unfound file") {
      val sut = new MutantFinder(new MutantMatcher)
      val noFile = FileUtil.getResource("scalaFiles/nonParseableFile.notScala")

      a[ParseException] should be thrownBy sut.parseFile(noFile)

      s"Error while parsing file '${noFile.relativePath}', expected class or object definition" should be(
        loggedAsError)
    }
  }
}
