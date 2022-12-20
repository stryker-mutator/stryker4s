package stryker4jvm.mutator.scala

import org.scalatest.funspec.AnyFunSpec
import stryker4jvm.mutator.scala.scalatest.FileUtil
import fs2.io.file.Path
import java.nio.file.NoSuchFileException

class ScalaParserTest extends AnyFunSpec {
  private val exampleClassFile = FileUtil.getResource("scalaFiles/ExampleClass.scala")

  describe("parseFile") {
    it("Should parse an existing file") {
      val sut = new ScalaParser()
      val file = exampleClassFile

      val result = sut.parse(file.toNioPath)

      val expected = """package stryker4jvm
                       |
                       |class ExampleClass {
                       |  def foo(num: Int) = num == 10
                       |
                       |  def createHugo = Person(22, "Hugo")
                       |}
                       |
                       |final case class Person(age: Int, name: String)
                       |""".stripMargin

      assert(result.syntax() == expected)
    }

    it("Should throw an exception on a non-parseable file") {
      val sut = new ScalaParser()
      val file = FileUtil.getResource("scalaFiles/nonParseableFile.notScala")

      assertThrows[Exception] {
        sut.parse(file.toNioPath)
      }
    }

    it("Should fail on a nonexistent file") {
      val sut = new ScalaParser()
      val noFile = Path("this/does/not/exist.scala")

      // sut.parse(noFile.toNioPath).assertThrows[NoSuchFileException]
      assertThrows[NoSuchFileException] {
        sut.parse(noFile.toNioPath)
      }
    }

  }
}
