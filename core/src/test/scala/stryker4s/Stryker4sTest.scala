package stryker4s

import java.nio.file.Paths

import stryker4s.config.Config
import stryker4s.model.{Killed, Mutant}
import stryker4s.mutants.Mutator
import stryker4s.mutants.applymutants.{MatchBuilder, StatementTransformer}
import stryker4s.mutants.findmutants.{MutantFinder, MutantMatcher}
import stryker4s.run.process.{Command, ProcessMutantRunner}
import stryker4s.run.threshold.SuccessStatus
import stryker4s.scalatest.{FileUtil, LogMatchers}
import stryker4s.testutil.stubs._
import stryker4s.testutil.Stryker4sSuite

import scala.util.Success

class Stryker4sTest extends Stryker4sSuite with LogMatchers {

  describe("run") {
    it("should call mutate files and report the results") {
      implicit val conf: Config = Config(baseDir = FileUtil.getResource("scalaFiles"))
      val file = FileUtil.getResource("scalaFiles/simpleFile.scala")
      val testFiles = Seq(file)
      val testSourceCollector = new TestSourceCollector(testFiles)
      val testProcessRunner = TestProcessRunner(Success(1), Success(1), Success(1), Success(1))
      val testMutantRunner = new ProcessMutantRunner(Command("foo", "test"), testProcessRunner)
      val testReporter = new TestReporter

      val sut = new Stryker4s(
        testSourceCollector,
        new Mutator(new MutantFinder(new MutantMatcher), new StatementTransformer, new MatchBuilder),
        testMutantRunner,
        testReporter
      )

      val result = sut.run()

      val reportedResults = testReporter.testMutantReporter.lastCall.value.results

      val expectedPath = Paths.get("simpleFile.scala")

      result shouldBe SuccessStatus
      reportedResults should matchPattern {
        case List(Killed(Mutant(0, _, _, _), `expectedPath`),
                  Killed(Mutant(1, _, _, _), `expectedPath`),
                  Killed(Mutant(2, _, _, _), `expectedPath`),
                  Killed(Mutant(3, _, _, _), `expectedPath`)) =>
      }
    }

    it("should log a warning when JVM max memory is too low") {
      implicit val conf: Config = Config()
      val testSourceCollector = new TestSourceCollector(Seq())
      val testProcessRunner = TestProcessRunner()
      val testMutantRunner = new ProcessMutantRunner(Command("foo", "test"), testProcessRunner)
      val testReporter = new TestReporter

      val sut = new TestStryker4s(
        jvmEnoughMemory = false,
        testSourceCollector,
        new Mutator(new MutantFinder(new MutantMatcher), new StatementTransformer, new MatchBuilder),
        testMutantRunner,
        testReporter
      )

      sut.run()

      "The JVM doesn't have a lot of memory assigned to it. " +
        "It's wise to increase the maximum memory when running stryker." shouldBe loggedAsWarning
      "This can be done in SBT by adding setting an environment variable: " +
        "SBT_OPTS=\"-XX:+CMSClassUnloadingEnabled -XX:MaxPermSize=4G -Xmx4G\" " shouldBe loggedAsWarning
    }
  }
}
