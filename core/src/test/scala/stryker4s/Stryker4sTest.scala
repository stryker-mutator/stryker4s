package stryker4s

// import scala.util.Success

import org.scalatest.Inside
// import stryker4s.config.Config
// import stryker4s.mutants.Mutator
// import stryker4s.mutants.applymutants.{ActiveMutationContext, MatchBuilder, StatementTransformer}
// import stryker4s.mutants.findmutants.{FileCollector, MutantFinder, MutantMatcher}
// import stryker4s.run.MutantRunner
// import stryker4s.scalatest.{FileUtil, LogMatchers}
// import stryker4s.testutil.stubs.{TestProcessRunner, TestRunnerStub, TestSourceCollector}
import stryker4s.testutil.{MockitoIOSuite, Stryker4sIOSuite}

class Stryker4sTest extends Stryker4sIOSuite with MockitoIOSuite with Inside {

  describe("run") {

    it("should call mutate files and report the results") {
      succeed
      // val file = FileUtil.getResource("scalaFiles/simpleFile.scala")
      // val testFiles = Seq(file)
      // val testSourceCollector = new TestSourceCollector(testFiles)
      // val testProcessRunner = TestProcessRunner(Success(1), Success(1), Success(1), Success(1))
      // val reporterMock = mock[AggregateReporter]
      // whenF(reporterMock.onRunFinished(any[FinishedRunEvent])).thenReturn(())
      // whenF(reporterMock.onMutationStart(any[StartMutationEvent])).thenReturn(())

      // implicit val conf: Config = Config(baseDir = FileUtil.getResource("scalaFiles"))

      // val testMutantRunner =
      //   new MutantRunner(TestRunnerStub.resource, new FileCollector(testProcessRunner), reporterMock)

      // val sut = new Stryker4s(
      //   testSourceCollector,
      //   new Mutator(
      //     new MutantFinder(new MutantMatcher),
      //     new StatementTransformer,
      //     new MatchBuilder(ActiveMutationContext.sysProps)
      //   ),
      //   testMutantRunner
      // )

      // sut.run().asserting { _ =>
      //   succeed
      // val startCaptor = ArgCaptor[StartMutationEvent]
      // verify(reporterMock, times(4)).onMutationStart(startCaptor)
      // startCaptor.values shouldBe List(
      //   StartMutationEvent(Progress(1, 4), 0),
      //   StartMutationEvent(Progress(2, 4), 0),
      //   StartMutationEvent(Progress(3, 4), 0),
      //   StartMutationEvent(Progress(4, 4), 0)
      // )
      // val runReportMock = ArgCaptor[FinishedRunEvent]
      // verify(reporterMock).onRunFinished(runReportMock)
      // val FinishedRunEvent(reportedResults, _, _, _) = runReportMock.value

      // reportedResults.files.flatMap(_._2.mutants) should have size 4
      // reportedResults.files.map { case (path, _) =>
      //   path shouldBe "simpleFile.scala"
      // }
      // result shouldBe SuccessStatus
      // }
    }
  }
}
