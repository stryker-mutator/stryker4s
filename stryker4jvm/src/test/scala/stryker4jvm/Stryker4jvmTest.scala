package stryker4jvm

import org.mockito.captor.ArgCaptor
import org.scalatest.Inside
import stryker4jvm.config.Config
import stryker4jvm.core.logging.Logger
import stryker4jvm.files.ConfigFilesResolver
import stryker4jvm.logging.FansiLogger
import stryker4jvm.mutants.{Mutator, SupportedLanguageMutators}
import stryker4jvm.reporting.FinishedRunEvent
import stryker4jvm.reporting.reporters.AggregateReporter
import stryker4jvm.run.threshold.SuccessStatus
import stryker4jvm.run.{MutantRunner, RollbackHandler}
import stryker4jvm.scalatest.{FileUtil, LogMatchers}
import stryker4jvm.testutil.{MockitoIOSuite, Stryker4jvmIOSuite, TestLanguageMutator, TestLogger}
import stryker4jvm.testutil.stubs.{TestFileResolver, TestProcessRunner, TestRunnerStub}

import scala.collection.immutable
import scala.util.Success

class Stryker4jvmTest extends Stryker4jvmIOSuite with MockitoIOSuite with Inside with LogMatchers {

  describe("run") {
    it("should call mutate files and report the results") {
      val log: Logger = new TestLogger(true)
      implicit val fansi: FansiLogger = new FansiLogger(log)

      val file = FileUtil.getResource("mockFiles/simple.test")
      val testFiles = immutable.Seq(file)
      val testSourceCollector = new TestFileResolver(testFiles)
      val testProcessRunner = TestProcessRunner(Success(1), Success(1), Success(1), Success(1))(fansi)
      val reporterMock = mock[AggregateReporter]
      val rollbackHandler = mock[RollbackHandler]
      when(reporterMock.mutantTested).thenReturn(_.drain)
      whenF(reporterMock.onRunFinished(any[FinishedRunEvent])).thenReturn(())

      implicit val conf: Config = Config.default.copy(baseDir = FileUtil.getResource("mockFiles"))

      val testMutantRunner =
        new MutantRunner(
          TestRunnerStub.resource,
          new ConfigFilesResolver(testProcessRunner)(conf, fansi),
          rollbackHandler,
          reporterMock
        )(conf, fansi)

      val testLanguageMutator = new TestLanguageMutator()
      val mutator = new Mutator(Map(".test" -> testLanguageMutator))(conf, fansi)

      val sut = new Stryker4jvm(
        testSourceCollector,
        mutator,
        testMutantRunner,
        reporterMock
      )

      sut.run().asserting { result =>
        val runReportMock = ArgCaptor[FinishedRunEvent]
        verify(reporterMock).onRunFinished(runReportMock)
        val FinishedRunEvent(reportedResults, _, _, _) = runReportMock.value

        reportedResults.files.flatMap(_._2.mutants) should have size 3
        reportedResults.files.loneElement._1 shouldBe "simple.test"
        result shouldBe SuccessStatus
      }
    }
  }
}
