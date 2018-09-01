package stryker4s.mutants.findmutants

import java.nio.file.NoSuchFileException

import better.files.File
import stryker4s.Stryker4sSuite
import stryker4s.run.MutantRegistry
import stryker4s.scalatest.{FileUtil, TreeEquality}

import scala.meta._
import scala.meta.parsers.ParseException

class MutantFinderTest extends Stryker4sSuite with TreeEquality {

  private val exampleClassFile = FileUtil.getResource("scalaFiles/ExampleClass.scala")

  describe("parseFile") {
    it("should parse an existing file") {
      val sut = new MutantFinder(new MutantMatcher, new MutantRegistry)
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
      val sut = new MutantFinder(new MutantMatcher, new MutantRegistry)
      val file = FileUtil.getResource("scalaFiles/nonParseableFile.notScala")

      lazy val result = sut.parseFile(file)

      a[ParseException] should be thrownBy result
    }

    it("should fail on a nonexistent file") {
      val sut = new MutantFinder(new MutantMatcher, new MutantRegistry)
      val noFile = File("this/does/not/exist.scala")

      lazy val result = sut.parseFile(noFile)

      a[NoSuchFileException] should be thrownBy result
    }
  }

  describe("findMutants") {
    it("should return empty list when given source has no possible mutations") {
      val sut = new MutantFinder(new MutantMatcher, new MutantRegistry)
      val source = source"case class Foo(s: String)"

      val result = sut.findMutants(source)

      result should be(empty)
    }

    it("should contain a mutant when given source has a possible mutation") {
      val sut = new MutantFinder(new MutantMatcher, new MutantRegistry)
      val source =
        source"""case class Bar(s: String) {
                    def foobar = s == "foobar"
                  }"""

      val result = sut.findMutants(source)

      val head = result.head
      head.originalStatement should equal(q"==")
      val firstMutant = head.mutants.loneElement
      firstMutant.original should equal(q"==")
      firstMutant.mutated should equal(q"!=")

      val secondMutant = result(1).mutants.loneElement
      result(1).originalStatement should equal(Lit.String("foobar"))
      secondMutant.original should equal(Lit.String("foobar"))
      secondMutant.mutated should equal(Lit.String(""))
    }
  }

  describe("mutantsInFile") {
    it("should return a FoundMutantsInSource with correct mutants") {
      val sut = new MutantFinder(new MutantMatcher, new MutantRegistry)
      val file = exampleClassFile

      val result = sut.mutantsInFile(file)

      result.source.children should not be empty
      val firstMutant = result.mutants.head
      firstMutant.originalStatement should equal(q"==")
      val firstLoneElement = firstMutant.mutants.loneElement
      firstLoneElement.original should equal(q"==")
      firstLoneElement.mutated should equal(q"!=")

      val secondMutant = result.mutants(1).mutants.loneElement
      result.mutants(1).originalStatement should equal(Lit.String("Hugo"))
      secondMutant.original should equal(Lit.String("Hugo"))
      secondMutant.mutated should equal(Lit.String(""))

    }

  }
}
