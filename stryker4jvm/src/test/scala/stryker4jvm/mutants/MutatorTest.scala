package stryker4jvm.mutants

import fansi.Color.*
import fs2.Stream
import stryker4jvm.config.Config
import stryker4jvm.core.config.LanguageMutatorConfig
import stryker4jvm.exception.InvalidFileTypeException
import stryker4jvm.scalatest.{FileUtil, LogMatchers}
import stryker4jvm.testutil.{MockAST, Stryker4jvmIOSuite, TestLanguageMutator}

import java.util

class MutatorTest extends Stryker4jvmIOSuite with LogMatchers {
  val testLanguageMutator = new TestLanguageMutator()
  implicit val conf: Config = Config.default
  val files = Stream(FileUtil.getResource("mockFiles/simple.test"))
  val mutator = new Mutator(Map(".test" -> testLanguageMutator))

  describe("go") {
    it("Should not give an error for correct files") {
      mutator.go(files).assertNoException
    }

    it("Should give an exception for invalid file extensions") {
      val mutator = new Mutator(Map(".notTest" -> testLanguageMutator))
      mutator.go(files).assertThrows[InvalidFileTypeException]
    }

    it("Should return the correctly mutated file") {
      val expected = new MockAST(
        "",
        Array(
          new MockAST("Mutated: Hello world"),
          new MockAST("not a mutator"),
          new MockAST(""),
          new MockAST("1 does not get mutated"),
          new MockAST("Mutated: Another mutator"),
          new MockAST("Mutated: Mut4t0r")
        )
      )
      mutator.go(files).asserting { case (_, result) =>
        assert(result.loneElement.mutatedSource.equals(expected))
      }
    }
  }

  describe("logs") {

    it("should log the amount of mutants found") {
      val testLanguageMutator = new TestLanguageMutator()
      val files = Stream(FileUtil.getResource("mockFiles/simple.test"))
      val sut = new Mutator(Map(".test" -> testLanguageMutator))
      sut.go(files).asserting { _ =>
        s"Found ${Cyan("1")} file(s) to be mutated." shouldBe loggedAsInfo
        s"Found ${Cyan("1")} file(s) to be mutated. Of which" should not be loggedAsInfo
        s"${Cyan("3")} mutant(s) generated" shouldBe loggedAsInfo
      }
    }

    it("should log the amount of excluded mutants") {
      val testLanguageMutator = new TestLanguageMutator()
      val excludedMutants: util.HashSet[String] = new util.HashSet()
      excludedMutants.add("noNumber")
      val testLanguageConfig = new LanguageMutatorConfig("test", excludedMutants)
      testLanguageMutator.collector.config = Config.default.copy(mutatorConfigs = Map("test" -> testLanguageConfig))
      val files = Stream(FileUtil.getResource("mockFiles/simple.test"))
      val sut = new Mutator(Map(".test" -> testLanguageMutator))

      sut.go(files).asserting { _ =>
        s"Found ${Cyan("1")} file(s) to be mutated." shouldBe loggedAsInfo
        s"${Cyan("3")} mutant(s) generated. Of which ${LightRed("1")} mutant(s) are excluded." shouldBe loggedAsInfo
        "Files to be mutated are found, but no mutations were found in those files." should not be loggedAsInfo
        "If this is not intended, please check your configuration and try again." should not be loggedAsInfo
      }
    }

    it("should log a warning if no mutants are found") {
      val testLanguageMutator = new TestLanguageMutator()
      val files = Stream(FileUtil.getResource("mockFiles/noMuts.test"))
      val sut = new Mutator(Map(".test" -> testLanguageMutator))

      sut.go(files).asserting { _ =>
        s"Found ${Cyan("1")} file(s) to be mutated." shouldBe loggedAsInfo
        s"${Cyan("0")} mutant(s) generated." shouldBe loggedAsInfo
        "Files to be mutated are found, but no mutations were found in those files." shouldBe loggedAsInfo
        "If this is not intended, please check your configuration and try again." shouldBe loggedAsInfo
      }
    }

    it("should log if all mutations are excluded") {
      val testLanguageMutator = new TestLanguageMutator()
      val excludedMutants: util.HashSet[String] = new util.HashSet()
      excludedMutants.add("noNumber")
      val testLanguageConfig = new LanguageMutatorConfig("test", excludedMutants)
      testLanguageMutator.collector.config = Config.default.copy(mutatorConfigs = Map("test" -> testLanguageConfig))
      val files = Stream(FileUtil.getResource("mockFiles/allExcluded.test"))
      val sut = new Mutator(Map(".test" -> testLanguageMutator))

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
