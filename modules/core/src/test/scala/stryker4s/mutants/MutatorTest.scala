package stryker4s.mutants

import fansi.Color.*
import fs2.Stream
import stryker4s.config.{Config, ExcludedMutation}
import stryker4s.mutants.findmutants.{MutantFinder, MutantMatcherImpl}
import stryker4s.mutants.tree.{InstrumenterOptions, MutantCollector, MutantInstrumenter}
import stryker4s.testkit.{FileUtil, LogMatchers, Stryker4sIOSuite}

import scala.meta.*

class MutatorTest extends Stryker4sIOSuite with LogMatchers {

  describe("run") {
    implicit val conf: Config = Config.default
    val sut = new Mutator(
      new MutantFinder(),
      new MutantCollector(new TreeTraverserImpl(), new MutantMatcherImpl()),
      new MutantInstrumenter(InstrumenterOptions.testRunner)
    )

    test("should return a single Tree with changed pattern match") {
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

        assertEquals(result.loneElement.mutatedSource, expected)
      }
    }

    test("should run go") {
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
        assertEquals(result.loneElement.mutatedSource, expected)
      }
    }
  }
  describe("logs") {

    test("should log the amount of mutants found") {
      implicit val conf: Config = Config.default
      def sut = new Mutator(
        new MutantFinder(),
        new MutantCollector(new TreeTraverserImpl, new MutantMatcherImpl),
        new MutantInstrumenter(InstrumenterOptions.testRunner)
      )
      val files = Stream(FileUtil.getResource("scalaFiles/simpleFile.scala"))

      sut.go(files).asserting { _ =>
        assertLoggedInfo(s"Found ${Cyan("1")} file(s) to be mutated.")
        assertNotLoggedInfo(s"Found ${Cyan("1")} file(s) to be mutated. Of which")
        assertLoggedInfo(s"${Cyan("4")} mutant(s) generated")
      }
    }

    test("should log the amount of excluded mutants") {
      implicit val conf: Config = Config.default.copy(excludedMutations = Seq(ExcludedMutation("EqualityOperator")))
      val sut = new Mutator(
        new MutantFinder(),
        new MutantCollector(new TreeTraverserImpl, new MutantMatcherImpl),
        new MutantInstrumenter(InstrumenterOptions.testRunner)
      )
      val files = Stream(FileUtil.getResource("scalaFiles/simpleFile.scala"))

      sut.go(files).asserting { _ =>
        assertLoggedInfo(s"Found ${Cyan("1")} file(s) to be mutated.")
        assertLoggedInfo(s"${Cyan("4")} mutant(s) generated. Of which ${LightRed("3")} mutant(s) are excluded.")
        assertNotLoggedInfo("Files to be mutated are found, but no mutations were found in those files.")
        assertNotLoggedInfo("If this is not intended, please check your configuration and try again.")
      }
    }

    test("should log a warning if no mutants are found") {
      implicit val conf: Config = Config.default.copy(excludedMutations = Seq(ExcludedMutation("EqualityOperator")))
      val sut = new Mutator(
        new MutantFinder(),
        new MutantCollector(new TreeTraverserImpl, new MutantMatcherImpl),
        new MutantInstrumenter(InstrumenterOptions.testRunner)
      )
      val files = Stream(FileUtil.getResource("fileTests/filledDir/src/main/scala/package/someFile.scala"))

      sut.go(files).asserting { _ =>
        assertLoggedInfo(s"Found ${Cyan("1")} file(s) to be mutated.")
        assertLoggedInfo(s"${Cyan("0")} mutant(s) generated.")
        assertLoggedInfo("Files to be mutated are found, but no mutations were found in those files.")
        assertLoggedInfo("If this is not intended, please check your configuration and try again.")
      }
    }

    test("should log if all mutations are excluded") {
      implicit val conf: Config = Config.default.copy(excludedMutations =
        Seq(ExcludedMutation("EqualityOperator"), ExcludedMutation("StringLiteral"))
      )
      val sut = new Mutator(
        new MutantFinder(),
        new MutantCollector(new TreeTraverserImpl, new MutantMatcherImpl),
        new MutantInstrumenter(InstrumenterOptions.testRunner)
      )
      val files = Stream(FileUtil.getResource("scalaFiles/simpleFile.scala"))

      sut.go(files).asserting { _ =>
        assertLoggedInfo(s"Found ${Cyan("1")} file(s) to be mutated.")
        assertLoggedInfo(s"${Cyan("4")} mutant(s) generated. Of which ${LightRed("4")} mutant(s) are excluded.")
        assertLoggedWarn(
          s"""All found mutations are excluded. Stryker4s will perform a dry-run without actually mutating anything.
             |You can configure the `mutate` or `excluded-mutations` property in your configuration""".stripMargin
        )
        assertNotLoggedInfo("Files to be mutated are found, but no mutations were found in those files.")
        assertNotLoggedInfo("If this is not intended, please check your configuration and try again.")
      }
    }
  }
}
