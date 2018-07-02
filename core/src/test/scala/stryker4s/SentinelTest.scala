package stryker4s

import java.nio.file.Paths

import stryker4s.config.Config
import stryker4s.model.{Killed, Mutant}
import stryker4s.mutants.Mutator
import stryker4s.mutants.applymutants.{MatchBuilder, StatementTransformer}
import stryker4s.mutants.findmutants.{MutantFinder, MutantMatcher}
import stryker4s.run.{MutantRegistry, ProcessMutantRunner}
import stryker4s.scalatest.FileUtil
import stryker4s.stubs.{TestMutantReporter, TestProcessRunner, TestSourceCollector}

import scala.meta._
import scala.util.Success

class Stryker4sTest extends Stryker4sSuite {
  describe("run") {
    it("should call mutate files and report the results") {
      implicit val conf: Config = Config(baseDir = FileUtil.getResource("scalaFiles"))
      val file = FileUtil.getResource("scalaFiles/simpleFile.scala")
      val testFiles = Seq(file)
      val testSourceCollector = new TestSourceCollector(testFiles)
      val testProcessRunner = new TestProcessRunner(Success(1), Success(1), Success(1))
      val testMutantRunner = new ProcessMutantRunner(testProcessRunner)
      val reporter = new TestMutantReporter

      val sut = new Stryker4s(testSourceCollector,
                             new Mutator(new MutantFinder(new MutantMatcher, new MutantRegistry),
                                         new StatementTransformer,
                                         new MatchBuilder),
                             testMutantRunner,
                             reporter)

      sut.run()
      val reportedResults = reporter.lastCall.value.results

      val expectedPath = Paths.get("simpleFile.scala")
      reportedResults should matchPattern {
        case List(Killed(1, Mutant(1, _, _), `expectedPath`),
                  Killed(1, Mutant(2, _, _), `expectedPath`),
                  Killed(1, Mutant(3, _, _), `expectedPath`)) =>
      }
    }
  }
}
