package stryker4s.mutants
import stryker4s.config.{Config, ExcludedMutations}
import stryker4s.mutants.applymutants.{ActiveMutationContext, MatchBuilder, StatementTransformer}
import stryker4s.mutants.findmutants.{MutantFinder, MutantMatcher}
import stryker4s.scalatest.{FileUtil, LogMatchers, TreeEquality}
import stryker4s.testutil.Stryker4sSuite
import stryker4s.testutil.stubs.TestSourceCollector

import scala.meta._

class MutatorTest extends Stryker4sSuite with TreeEquality with LogMatchers {

  describe("run") {
    it("should return a single Tree with changed pattern match") {
      implicit val conf: Config = Config()
      val files = new TestSourceCollector(Seq(FileUtil.getResource("scalaFiles/simpleFile.scala")))
        .collectFilesToMutate()

      val sut = new Mutator(
        new MutantFinder(new MutantMatcher),
        new StatementTransformer,
        new MatchBuilder(ActiveMutationContext.sysProps)
      )

      val result = sut.mutate(files)

      val expected = """object Foo {
                       |  def bar = sys.props.get("ACTIVE_MUTATION") match {
                       |    case Some("0") =>
                       |      15 >= 14
                       |    case Some("1") =>
                       |      15 < 14
                       |    case Some("2") =>
                       |      15 == 14
                       |    case _ =>
                       |      15 > 14
                       |  }
                       |  def foobar = sys.props.get("ACTIVE_MUTATION") match {
                       |    case Some("3") =>
                       |      s""
                       |    case _ =>
                       |      s"${{
                       |        bar
                       |      }}foo"
                       |  }
                       |}""".stripMargin.parse[Source].get
      result.loneElement.tree should equal(expected)
    }
  }
  describe("logs") {
    it("should log the amount of mutants found") {
      implicit val conf: Config = Config()
      val files = new TestSourceCollector(Seq(FileUtil.getResource("scalaFiles/simpleFile.scala")))
        .collectFilesToMutate()

      val sut = new Mutator(
        new MutantFinder(new MutantMatcher),
        new StatementTransformer,
        new MatchBuilder(ActiveMutationContext.sysProps)
      )

      sut.mutate(files)

      "Found 1 of 1 file(s) to be mutated." shouldBe loggedAsInfo
      "4 Mutant(s) generated" shouldBe loggedAsInfo
    }

    it("should log the amount of excluded mutants") {
      implicit val conf: Config = Config(excludedMutations = ExcludedMutations(Set("EqualityOperator")))
      val files = new TestSourceCollector(Seq(FileUtil.getResource("scalaFiles/simpleFile.scala")))
        .collectFilesToMutate()

      val sut = new Mutator(
        new MutantFinder(new MutantMatcher),
        new StatementTransformer,
        new MatchBuilder(ActiveMutationContext.sysProps)
      )

      sut.mutate(files)

      "Found 1 of 1 file(s) to be mutated." shouldBe loggedAsInfo
      "4 Mutant(s) generated. Of which 3 Mutant(s) are excluded." shouldBe loggedAsInfo
      "Files to be mutated are found, but no mutations were found in those files." should not be loggedAsInfo
      "If this is not intended, please check your configuration and try again." should not be loggedAsInfo
    }

    it("should log a warning if no mutants are found") {
      implicit val conf: Config = Config()
      val files =
        new TestSourceCollector(Seq(FileUtil.getResource("fileTests/filledDir/src/main/scala/package/someFile.scala")))
          .collectFilesToMutate()

      val sut = new Mutator(
        new MutantFinder(new MutantMatcher),
        new StatementTransformer,
        new MatchBuilder(ActiveMutationContext.sysProps)
      )

      sut.mutate(files)

      "Found 0 of 1 file(s) to be mutated." shouldBe loggedAsInfo
      "0 Mutant(s) generated." shouldBe loggedAsInfo
      "Files to be mutated are found, but no mutations were found in those files." shouldBe loggedAsInfo
      "If this is not intended, please check your configuration and try again." shouldBe loggedAsInfo
    }

    it("should log if no files are found") {
      implicit val conf: Config = Config()
      val files = new TestSourceCollector(Seq.empty)
        .collectFilesToMutate()

      val sut = new Mutator(
        new MutantFinder(new MutantMatcher),
        new StatementTransformer,
        new MatchBuilder(ActiveMutationContext.sysProps)
      )

      sut.mutate(files)

      "Found 0 of 0 file(s) to be mutated." shouldBe loggedAsInfo
      "0 Mutant(s) generated." shouldBe loggedAsInfo
      """No files marked to be mutated. Stryker4s will perform a dry-run without actually mutating anything.
      |You can configure the `mutate` property in your configuration""".stripMargin shouldBe loggedAsWarning

      "Files to be mutated are found, but no mutations were found in those files." should not be loggedAsInfo
      "If this is not intended, please check your configuration and try again." should not be loggedAsInfo
    }

    it("should log if all mutations are excluded") {
      implicit val conf: Config =
        Config(excludedMutations = ExcludedMutations(Set("EqualityOperator", "StringLiteral")))
      val files = new TestSourceCollector(Seq(FileUtil.getResource("scalaFiles/simpleFile.scala")))
        .collectFilesToMutate()

      val sut = new Mutator(
        new MutantFinder(new MutantMatcher),
        new StatementTransformer,
        new MatchBuilder(ActiveMutationContext.sysProps)
      )

      sut.mutate(files)

      "Found 1 of 1 file(s) to be mutated." shouldBe loggedAsInfo
      "4 Mutant(s) generated. Of which 4 Mutant(s) are excluded." shouldBe loggedAsInfo
      s"""All found mutations are excluded. Stryker4s will perform a dry-run without actually mutating anything.
       |You can configure the `excluded-mutations` property in your configuration""".stripMargin shouldBe loggedAsWarning

      """No files marked to be mutated. Stryker4s will perform a dry-run without actually mutating anything.
        |You can configure the `mutate` property in your configuration""".stripMargin should not be loggedAsWarning
      "Files to be mutated are found, but no mutations were found in those files." should not be loggedAsInfo
      "If this is not intended, please check your configuration and try again." should not be loggedAsInfo
    }
  }
}
