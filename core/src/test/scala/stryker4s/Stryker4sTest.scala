package stryker4s

import java.nio.file.Paths

import org.mockito.ArgumentMatchersSugar
import org.mockito.Mockito._
import org.scalatest.mockito.MockitoSugar
import stryker4s.config.Config
import stryker4s.model.{Killed, Mutant, MutantRunResults}
import stryker4s.mutants.Mutator
import stryker4s.mutants.applymutants.{MatchBuilder, StatementTransformer}
import stryker4s.mutants.findmutants.{MutantFinder, MutantMatcher}
import stryker4s.run.ProcessMutantRunner
import stryker4s.run.process.Command
import stryker4s.run.report.{ConsoleReporter, HtmlReporter, Reporter}
import stryker4s.scalatest.FileUtil
import stryker4s.stubs.{TestProcessRunner, TestReporter, TestSourceCollector}

import scala.util.Success

class Stryker4sTest extends Stryker4sSuite with MockitoSugar with ArgumentMatchersSugar {

  describe("run") {
    val file = FileUtil.getResource("scalaFiles/simpleFile.scala")
    val testFiles = Seq(file)
    val testSourceCollector = new TestSourceCollector(testFiles)
    val testProcessRunner = new TestProcessRunner(Success(1), Success(1), Success(1))

    it("should call mutate files and report the results") {
      implicit val conf: Config = Config(baseDir = FileUtil.getResource("scalaFiles"))

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

      sut.run()
      val reportedResults = testReporter.testMutantReporter.lastCall.value.results

      val expectedPath = Paths.get("simpleFile.scala")
      reportedResults should matchPattern {
        case List(Killed(1, Mutant(0, _, _, _), `expectedPath`),
                  Killed(1, Mutant(1, _, _, _), `expectedPath`),
                  Killed(1, Mutant(2, _, _, _), `expectedPath`)) =>
      }
    }

    it("should call multiple reporters when more then one is specified") {
      val consoleReporterMock: ConsoleReporter = mock[ConsoleReporter]
      val htmlReporterMock: HtmlReporter = mock[HtmlReporter]

      implicit val conf: Config = Config(reporters = List(consoleReporterMock, htmlReporterMock))
      val testMutantRunner = new ProcessMutantRunner(Command("foo", "test"), testProcessRunner)

      val sut = new Stryker4s(
        testSourceCollector,
        new Mutator(new MutantFinder(new MutantMatcher),
                    new StatementTransformer,
                    new MatchBuilder),
        testMutantRunner,
        new Reporter
      )

      sut.run()

      verify(consoleReporterMock).report(any[MutantRunResults])(any[Config])
      verify(htmlReporterMock).report(any[MutantRunResults])(any[Config])
    }
  }
}
