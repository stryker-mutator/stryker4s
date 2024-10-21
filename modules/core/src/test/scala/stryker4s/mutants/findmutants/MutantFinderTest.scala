package stryker4s.mutants.findmutants

import fs2.io.file.Path
import stryker4s.config.Config
import stryker4s.extension.FileExtensions.*
import stryker4s.log.Logger
import stryker4s.testkit.{FileUtil, LogMatchers, Stryker4sIOSuite}

import java.nio.file.NoSuchFileException
import scala.meta.*
import scala.meta.parsers.ParseException

class MutantFinderTest extends Stryker4sIOSuite with LogMatchers {
  private val exampleClassFile = FileUtil.getResource("scalaFiles/ExampleClass.scala")

  describe("parseFile") {
    implicit val config: Config = Config.default
    test("should parse an existing file") {

      val sut = new MutantFinder()
      val file = exampleClassFile

      sut.parseFile(file).asserting { result =>
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
        assertEquals(result, expected)
      }
    }

    test("should throw an exception on a non-parseable file") {
      val sut = new MutantFinder()
      val file = FileUtil.getResource("scalaFiles/nonParseableFile.notScala")

      sut.parseFile(file).intercept[ParseException].asserting { err =>
        assertEquals(err.shortMessage, "illegal start of definition `identifier`")
      }
    }

    test("should fail on a nonexistent file") {
      val sut = new MutantFinder()
      val noFile = Path("this/does/not/exist.scala")

      sut.parseFile(noFile).intercept[NoSuchFileException]
    }

    test("should parse a scala-3 file") {
      import scala.meta.dialects.Scala3

      val scala3DialectConfig = config.copy(scalaDialect = Scala3)
      val sut = new MutantFinder()(scala3DialectConfig, implicitly[Logger])
      val file = FileUtil.getResource("scalaFiles/scala3File.scala")

      sut.parseFile(file).void.assert
    }
  }

  describe("logging") {
    test("should error log an unfound file") {
      implicit val config: Config = Config.default

      val sut = new MutantFinder()
      val noFile = FileUtil.getResource("scalaFiles/nonParseableFile.notScala")

      sut
        .parseFile(noFile)
        .intercept[ParseException]
        .asserting { _ =>
          assertLoggedError(
            s"Error while parsing file '${noFile.relativePath}', illegal start of definition `identifier`"
          )
        }
    }
  }
}
