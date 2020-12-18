package stryker4s

import scala.meta._
import scala.util.Success

import better.files.File
import cats.effect.{IO, Resource}
import org.mockito.captor.ArgCaptor
import org.scalatest.Inside
import stryker4s.config.Config
import stryker4s.extension.mutationtype.LesserThan
import stryker4s.model.{Killed, Mutant, MutantRunResult, TestRunnerContext}
import stryker4s.mutants.Mutator
import stryker4s.mutants.applymutants.{ActiveMutationContext, MatchBuilder, StatementTransformer}
import stryker4s.mutants.findmutants.{FileCollector, MutantFinder, MutantMatcher, SourceCollector}
import stryker4s.report.{AggregateReporter, FinishedRunEvent, Progress, Reporter, StartMutationEvent}
import stryker4s.run.threshold.SuccessStatus
import stryker4s.run.{InitialTestRunResult, MutantRunner}
import stryker4s.scalatest.{FileUtil, LogMatchers}
import stryker4s.testutil.stubs.{TestProcessRunner, TestSourceCollector}
import stryker4s.testutil.{MockitoIOSuite, Stryker4sIOSuite}

class Stryker4sTest extends Stryker4sIOSuite with MockitoIOSuite with Inside with LogMatchers {

  case class TestTestRunnerContext() extends TestRunnerContext
  class TestMutantRunner(sourceCollector: SourceCollector, reporter: Reporter)(implicit config: Config)
      extends MutantRunner(sourceCollector, reporter) {
    private[this] val stream = Iterator.from(0)
    type Context = TestTestRunnerContext
    override def runMutant(mutant: Mutant, context: Context): IO[MutantRunResult] =
      IO.pure(Killed(Mutant(stream.next(), q">", q"<", LesserThan)))
    override def runInitialTest(context: Context): IO[InitialTestRunResult] = IO.pure(Left(true))
    override def initializeTestContext(tmpDir: File): Resource[IO, Context] =
      Resource.pure[IO, Context](TestTestRunnerContext())
  }

  describe("run") {

    it("should call mutate files and report the results") {
      val file = FileUtil.getResource("scalaFiles/simpleFile.scala")
      val testFiles = Seq(file)
      val testSourceCollector = new TestSourceCollector(testFiles)
      val testProcessRunner = TestProcessRunner(Success(1), Success(1), Success(1), Success(1))
      val reporterMock = mock[AggregateReporter]
      whenF(reporterMock.onRunFinished(any[FinishedRunEvent])).thenReturn(())
      whenF(reporterMock.onMutationStart(any[StartMutationEvent])).thenReturn(())

      implicit val conf: Config = Config(baseDir = FileUtil.getResource("scalaFiles"))

      val testMutantRunner = new TestMutantRunner(new FileCollector(testProcessRunner), reporterMock)

      val sut = new Stryker4s(
        testSourceCollector,
        new Mutator(
          new MutantFinder(new MutantMatcher),
          new StatementTransformer,
          new MatchBuilder(ActiveMutationContext.sysProps)
        ),
        testMutantRunner
      )

      sut.run().asserting { result =>
        val startCaptor = ArgCaptor[StartMutationEvent]
        verify(reporterMock, times(4)).onMutationStart(startCaptor)
        startCaptor.values should matchPattern {
          case List(
                StartMutationEvent(Progress(1, 4)),
                StartMutationEvent(Progress(2, 4)),
                StartMutationEvent(Progress(3, 4)),
                StartMutationEvent(Progress(4, 4))
              ) =>
        }
        val runReportMock = ArgCaptor[FinishedRunEvent]
        verify(reporterMock).onRunFinished(runReportMock)
        val FinishedRunEvent(reportedResults, _, _, _) = runReportMock.value

        reportedResults.files.flatMap(_._2.mutants) should have size 4
        reportedResults.files.map { case (path, _) =>
          path shouldBe "simpleFile.scala"
        }
        result shouldBe SuccessStatus
      }
    }
  }
}
