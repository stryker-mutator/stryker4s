package stryker4s

import org.mockito.captor.ArgCaptor
import org.scalatest.Inside
import stryker4s.config.Config
import stryker4s.files.ConfigFilesResolver
import stryker4s.mutants.Mutator
import stryker4s.mutants.applymutants.{ActiveMutationContext, MatchBuilder, StatementTransformer}
import stryker4s.mutants.findmutants.{MutantFinder, MutantMatcher}
import stryker4s.report.{AggregateReporter, FinishedRunEvent}
import stryker4s.run.MutantRunner
import stryker4s.run.threshold.SuccessStatus
import stryker4s.scalatest.{FileUtil, LogMatchers}
import stryker4s.testutil.stubs.{TestFileResolver, TestProcessRunner, TestRunnerStub}
import stryker4s.testutil.{MockitoIOSuite, Stryker4sIOSuite}

import scala.util.Success

class Stryker4sTest extends Stryker4sIOSuite with MockitoIOSuite with Inside with LogMatchers {

  describe("run") {

    it("should call mutate files and report the results") {
      val file = FileUtil.getResource("scalaFiles/simpleFile.scala")
      val testFiles = Seq(file)
      val testSourceCollector = new TestFileResolver(testFiles)
      val testProcessRunner = TestProcessRunner(Success(1), Success(1), Success(1), Success(1))
      val reporterMock = mock[AggregateReporter]
      when(reporterMock.mutantTested).thenReturn(_.drain)
      whenF(reporterMock.onRunFinished(any[FinishedRunEvent])).thenReturn(())

      implicit val conf: Config = Config.default.copy(baseDir = FileUtil.getResource("scalaFiles"))

      val testMutantRunner =
        new MutantRunner(TestRunnerStub.resource, new ConfigFilesResolver(testProcessRunner), reporterMock)

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
