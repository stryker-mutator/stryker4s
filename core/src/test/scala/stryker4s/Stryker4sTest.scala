package stryker4s

import java.nio.file.Paths

import stryker4s.config.Config
import stryker4s.model.{Killed, Mutant}
import stryker4s.mutants.Mutator
import stryker4s.mutants.applymutants.{MatchBuilder, StatementTransformer}
import stryker4s.mutants.findmutants.{MutantFinder, MutantMatcher}
import stryker4s.run.ProcessMutantRunner
import stryker4s.run.process.Command
import stryker4s.scalatest.FileUtil
import stryker4s.stubs.{TestProcessRunner, TestReporter, TestSourceCollector}

import scala.util.Success

class Stryker4sTest extends Stryker4sSuite {

  describe("run") {
    it("should call mutate files and report the results") {
      implicit val conf: Config = Config(baseDir = FileUtil.getResource("scalaFiles"))
      val file = FileUtil.getResource("scalaFiles/simpleFile.scala")
      val testFiles = Seq(file)
      val testSourceCollector = new TestSourceCollector(testFiles)
      val testProcessRunner = new TestProcessRunner(Success(1), Success(1), Success(1), Success(1))
      val testMutantRunner = new ProcessMutantRunner(Command("foo", "test"), testProcessRunner)
      val testReporter = new TestReporter

      val sut = new Stryker4s(
        testSourceCollector,
        new Mutator(new MutantFinder(new MutantMatcher),
                    new StatementTransformer,
                    new MatchBuilder),
        testMutantRunner,
        testReporter
      )

      val exitCode = sut.run()

      val reportedResults = testReporter.testMutantReporter.lastCall.value.results

      val expectedPath = Paths.get("simpleFile.scala")

      exitCode shouldBe 0
      reportedResults should matchPattern {
        case List(Killed(1, Mutant(0, _, _, _), `expectedPath`),
                  Killed(1, Mutant(1, _, _, _), `expectedPath`),
                  Killed(1, Mutant(2, _, _, _), `expectedPath`),
                  Killed(1, Mutant(3, _, _, _), `expectedPath`)) =>
      }
    }
  }
}
