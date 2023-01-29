package stryker4jvm.mutator.scala

import stryker4jvm.mutator.scala.testutil.Stryker4jvmSuite
import stryker4jvm.mutator.scala.scalatest.FileUtil
import fs2.io.file.Path
import stryker4jvm.mutator.scala.extensions.TreeExtensions.IsEqualExtension

import java.nio.file.NoSuchFileException
import scala.meta.{dialects, Dialect, Source, XtensionParseInputLike}

class ScalaParserTest extends Stryker4jvmSuite {
  private val exampleClassFile = FileUtil.getResource("scalaFiles/ExampleClass.scala")

  describe("parseFile") {
    it("Should parse an existing file") {
      val sut = new ScalaParser(dialects.Scala213Source3)
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
                       |""".stripMargin.parse[Source].get

      assert(result.value.isEqual(expected), result.value)
    }

    it("Should throw an exception on a non-parseable file") {
      val sut = new ScalaParser(dialects.Scala213Source3)
      val file = FileUtil.getResource("scalaFiles/nonParseableFile.notScala")

      assertThrows[Exception] {
        sut.parse(file.toNioPath)
      }
    }

    it("Should fail on a nonexistent file") {
      val sut = new ScalaParser(dialects.Scala213Source3)
      val noFile = Path("this/does/not/exist.scala")

      // sut.parse(noFile.toNioPath).assertThrows[NoSuchFileException]
      assertThrows[NoSuchFileException] {
        sut.parse(noFile.toNioPath)
      }
    }

  }
}
