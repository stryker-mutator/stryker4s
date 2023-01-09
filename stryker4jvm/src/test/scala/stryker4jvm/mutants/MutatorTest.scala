package stryker4jvm.mutants

import fansi.Color.*
import fs2.Stream
import stryker4jvm.config.Config
import stryker4jvm.scalatest.{FileUtil, LogMatchers}
import stryker4jvm.testutil.Stryker4jvmIOSuite

import scala.meta.*

class MutatorTest extends Stryker4jvmIOSuite with LogMatchers {

  describe("run") {
    implicit val conf: Config = Config.default
    val sut = new Mutator(
      SupportedLanguageMutators.languageRouter
    )
    // TODO MutatorTest tests

    it("should return a single AST with changed pattern match") {
      val files = Stream(FileUtil.getResource("scalaFiles/simpleFile.scala"))

      sut.go(files).asserting { case (_, result) =>
        val expected = """object Foo {
                         |  def bar = _root_.stryker4s.activeMutation match {
                         |    case 0 =>
                         |      15 >= 14
                         |    case 1 =>
                         |      15 < 14
                         |    case 2 =>
                         |      15 == 14
                         |    case _ if _root_.stryker4s.coverage.coverMutant(0, 1, 2) =>
                         |      15 > 14
                         |  }
                         |  def foobar = _root_.stryker4s.activeMutation match {
                         |    case 3 =>
                         |      ""
                         |    case _ if _root_.stryker4s.coverage.coverMutant(3) =>
                         |      s"${bar}foo"
                         |  }
                         |}""".stripMargin.parse[Source].get

        assert(result.loneElement.mutatedSource.equals(expected), result.loneElement.mutatedSource)
      }
    }

    it("should run go") {
      val files = Stream(FileUtil.getResource("scalaFiles/simpleFile.scala"))

      sut.go(files).asserting { case (_, result) =>
        val expected = """object Foo {
                         |  def bar = _root_.stryker4s.activeMutation match {
                         |    case 0 =>
                         |      15 >= 14
                         |    case 1 =>
                         |      15 < 14
                         |    case 2 =>
                         |      15 == 14
                         |    case _ if _root_.stryker4s.coverage.coverMutant(0, 1, 2) =>
                         |      15 > 14
                         |  }
                         |  def foobar = _root_.stryker4s.activeMutation match {
                         |    case 3 =>
                         |      ""
                         |    case _ if _root_.stryker4s.coverage.coverMutant(3) =>
                         |      s"${bar}foo"
                         |  }
                         |}""".stripMargin.parse[Source].get
        assert(result.loneElement.mutatedSource.equals(expected), result.loneElement.mutatedSource)
      }
    }
  }
  describe("logs") {

    it("should log the amount of mutants found") {
      implicit val conf: Config = Config.default
      val sut = new Mutator(
        SupportedLanguageMutators.languageRouter
      )
      val files = Stream(FileUtil.getResource("scalaFiles/simpleFile.scala"))

      sut.go(files).asserting { _ =>
        s"Found ${Cyan("1")} file(s) to be mutated." shouldBe loggedAsInfo
        s"Found ${Cyan("1")} file(s) to be mutated. Of which" should not be loggedAsInfo
        s"${Cyan("4")} mutant(s) generated" shouldBe loggedAsInfo
      }
    }

    it("should log the amount of excluded mutants") {
      implicit val conf: Config = Config.default.copy(excludedMutations = Set("EqualityOperator"))
      val sut = new Mutator(
        SupportedLanguageMutators.languageRouter
      )
      val files = Stream(FileUtil.getResource("scalaFiles/simpleFile.scala"))

      sut.go(files).asserting { _ =>
        s"Found ${Cyan("1")} file(s) to be mutated." shouldBe loggedAsInfo
        s"${Cyan("4")} mutant(s) generated. Of which ${LightRed("3")} mutant(s) are excluded." shouldBe loggedAsInfo
        "Files to be mutated are found, but no mutations were found in those files." should not be loggedAsInfo
        "If this is not intended, please check your configuration and try again." should not be loggedAsInfo
      }
    }

    it("should log a warning if no mutants are found") {
      implicit val conf: Config = Config.default.copy(excludedMutations = Set("EqualityOperator"))
      val sut = new Mutator(
        SupportedLanguageMutators.languageRouter
      )
      val files = Stream(FileUtil.getResource("fileTests/filledDir/src/main/scala/package/someFile.scala"))

      sut.go(files).asserting { _ =>
        s"Found ${Cyan("1")} file(s) to be mutated." shouldBe loggedAsInfo
        s"${Cyan("0")} mutant(s) generated." shouldBe loggedAsInfo
        "Files to be mutated are found, but no mutations were found in those files." shouldBe loggedAsInfo
        "If this is not intended, please check your configuration and try again." shouldBe loggedAsInfo
      }
    }

    it("should log if all mutations are excluded") {
      implicit val conf: Config = Config.default.copy(excludedMutations = Set("EqualityOperator", "StringLiteral"))
      val sut = new Mutator(
        SupportedLanguageMutators.languageRouter
      )
      val files = Stream(FileUtil.getResource("scalaFiles/simpleFile.scala"))

      sut.go(files).asserting { _ =>
        s"Found ${Cyan("1")} file(s) to be mutated." shouldBe loggedAsInfo
        s"${Cyan("4")} mutant(s) generated. Of which ${LightRed("4")} mutant(s) are excluded." shouldBe loggedAsInfo
        s"""All found mutations are excluded. Stryker4s will perform a dry-run without actually mutating anything.
           |You can configure the `mutate` or `excluded-mutations` property in your configuration""".stripMargin shouldBe loggedAsWarning
        "Files to be mutated are found, but no mutations were found in those files." should not be loggedAsInfo
        "If this is not intended, please check your configuration and try again." should not be loggedAsInfo
      }
    }
  }

}

final class IsEqualExtension(val thisTree: Tree) extends AnyVal {

  /** Structural equality for Trees
    */
  final def isEqual(other: Tree): Boolean = thisTree == other || thisTree.structure == other.structure
}
