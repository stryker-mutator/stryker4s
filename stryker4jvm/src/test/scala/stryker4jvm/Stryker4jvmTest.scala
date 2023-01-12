package stryker4jvm

import org.mockito.captor.ArgCaptor
import org.scalatest.Inside
import stryker4jvm.config.Config
import stryker4jvm.core.logging.Logger
import stryker4jvm.files.ConfigFilesResolver
import stryker4jvm.mutants.{Mutator, SupportedLanguageMutators}
import stryker4jvm.reporting.FinishedRunEvent
import stryker4jvm.reporting.reporters.AggregateReporter
import stryker4jvm.run.threshold.SuccessStatus
import stryker4jvm.run.{MutantRunner, RollbackHandler}
import stryker4jvm.scalatest.{FileUtil, LogMatchers}
import stryker4jvm.testutil.{MockitoIOSuite, Stryker4jvmIOSuite, TestLogger}
import stryker4jvm.testutil.stubs.{TestFileResolver, TestProcessRunner}

import scala.collection.immutable
import scala.util.Success

class Stryker4jvmTest extends Stryker4jvmIOSuite with MockitoIOSuite with Inside with LogMatchers {

  describe("run") {
//    TODO: Test Stryker4jvm.scala without mutator-scala
//    it("should call mutate files and report the results") {
//      implicit val log : Logger = new TestLogger(true)
//
//      val file = FileUtil.getResource("scalaFiles/simpleFile.scala")
//      val testFiles = immutable.Seq(file)
//      val testSourceCollector = new TestFileResolver(testFiles)
//      val testProcessRunner = TestProcessRunner(Success(1), Success(1), Success(1), Success(1))(log)
//      val reporterMock = mock[AggregateReporter]
//      val rollbackHandler = mock[RollbackHandler]
//      when(reporterMock.mutantTested).thenReturn(_.drain)
//      whenF(reporterMock.onRunFinished(any[FinishedRunEvent])).thenReturn(())
//
//      implicit val conf: Config = Config.default.copy(baseDir = FileUtil.getResource("scalaFiles"))
//
//      val testMutantRunner =
//        new MutantRunner(
//          TestRunnerStub.resource,
//          new ConfigFilesResolver(testProcessRunner)(conf, log),
//          rollbackHandler,
//          reporterMock
//        )(conf, Logger)
//
//      val sut = new Stryker4jvm(
//        testSourceCollector,
//        new Mutator(
//          SupportedLanguageMutators.languageRouter
//        )(conf, Logger),
//        testMutantRunner,
//        reporterMock
//      )
//
//      sut.run().asserting { result =>
//        val runReportMock = ArgCaptor[FinishedRunEvent]
//        verify(reporterMock).onRunFinished(runReportMock)
//        val FinishedRunEvent(reportedResults, _, _, _) = runReportMock.value
//
//        reportedResults.files.flatMap(_._2.mutants) should have size 4
//        reportedResults.files.loneElement._1 shouldBe "simpleFile.scala"
//        result shouldBe SuccessStatus
//      }
//    }
  }
}
