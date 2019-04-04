package stryker4s

import java.nio.file.{Path, Paths}

import better.files.File
import stryker4s.config.Config
import stryker4s.model.{Killed, Mutant, MutantRunResult}
import org.mockito.captor.ArgCaptor
import org.mockito.{ArgumentMatchersSugar, MockitoSugar}
import org.scalatest.Inside
import stryker4s.config.{Config, ConsoleReporterType, HtmlReporterType}
import stryker4s.model.{Killed, Mutant, MutantRunResults}
import stryker4s.mutants.Mutator
import stryker4s.mutants.applymutants.{ActiveMutationContext, MatchBuilder, StatementTransformer}
import stryker4s.mutants.findmutants.{FileCollector, MutantFinder, MutantMatcher, SourceCollector}
import stryker4s.run.MutantRunner
import stryker4s.mutants.findmutants.{FileCollector, MutantFinder, MutantMatcher}
import stryker4s.report.Reporter
import stryker4s.run.process.{Command, ProcessMutantRunner}
import stryker4s.run.threshold.SuccessStatus
import stryker4s.scalatest.{FileUtil, LogMatchers}
import stryker4s.testutil.Stryker4sSuite
import stryker4s.testutil.stubs.{TestProcessRunner, TestSourceCollector}

import scala.util.Success

class Stryker4sTest extends Stryker4sSuite with MockitoSugar with ArgumentMatchersSugar with Inside with LogMatchers {

  class TestMutantRunner(sourceCollector: SourceCollector)(implicit config: Config)
      extends MutantRunner(sourceCollector) {
    private[this] val stream = Iterator.from(0)

    override def runMutant(mutant: Mutant, workingDir: File): Path => MutantRunResult =
      path => Killed(Mutant(stream.next, null, null, null), path)
    override def runInitialTest(workingDir: File): Boolean = true
  }

  describe("run") {
    val file = FileUtil.getResource("scalaFiles/simpleFile.scala")
    val testFiles = Seq(file)
    val testSourceCollector = new TestSourceCollector(testFiles)
    val testProcessRunner = TestProcessRunner(Success(1), Success(1), Success(1), Success(1))
    val reporterMock = mock[Reporter]

    it("should call mutate files and report the results") {
      implicit val conf: Config = Config(baseDir = FileUtil.getResource("scalaFiles"))

      val testMutantRunner = new TestMutantRunner(new FileCollector(testProcessRunner),

      reporterMock)

      val sut = new Stryker4s(
        testSourceCollector,
        new Mutator(new MutantFinder(new MutantMatcher),
                    new StatementTransformer,
                    new MatchBuilder(ActiveMutationContext.sysProps)),
        testMutantRunner
      )

      val result = sut.run()

      val startCaptor = ArgCaptor[Mutant]
      verify(reporterMock, times(4)).reportMutationStart(startCaptor)
      startCaptor.values should matchPattern {
        case List(Mutant(0, _, _, _), Mutant(1, _, _, _), Mutant(2, _, _, _), Mutant(3, _, _, _)) =>
      }
      val runResultCaptor = ArgCaptor[MutantRunResults]
      verify(reporterMock).reportRunFinished(runResultCaptor)
      val reportedResults = runResultCaptor.value.results

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
      val testMutantRunner =
        new ProcessMutantRunner(Command("foo", "test"), testProcessRunner, testSourceCollector, reporterMock)

      val sut: Stryker4s =
        new Stryker4s(
          testSourceCollector,
          new Mutator(new MutantFinder(new MutantMatcher),
                      new StatementTransformer,
                      new MatchBuilder(ActiveMutationContext.sysProps)),
          testMutantRunner
        ) {

          override def jvmMemory2GBOrHigher: Boolean = false
        }

      sut.run()

      "The JVM has less than 2GB memory available. We advise to allocate 4GB memory when running Stryker4s." shouldBe loggedAsWarning
      "Visit https://github.com/stryker-mutator/stryker4s#memory-usage for more info on how to allocate more memory to the JVM." shouldBe loggedAsWarning
    }

    it("should not log a warning when JVM max memory is high enough") {
      implicit val conf: Config = Config()
      val testMutantRunner =
        new ProcessMutantRunner(Command("foo", "test"), testProcessRunner, testSourceCollector, reporterMock)

      val sut: Stryker4s =
        new Stryker4s(
          testSourceCollector,
          new Mutator(new MutantFinder(new MutantMatcher),
                      new StatementTransformer,
                      new MatchBuilder(ActiveMutationContext.sysProps)),
          testMutantRunner
        ) {

          override def jvmMemory2GBOrHigher: Boolean = true
        }

      sut.run()

      "The JVM has less than 2GB memory available. We advise to allocate 4GB memory when running Stryker4s." should not be loggedAsWarning
      "Visit https://github.com/stryker-mutator/stryker4s#memory-usage for more info on how to allocate more memory to the JVM." should not be loggedAsWarning
    }
  }
}
