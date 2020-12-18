package stryker4s.report

import scala.concurrent.duration._

import better.files.File
import mutationtesting._
import stryker4s.scalatest.LogMatchers
import stryker4s.testutil.{MockitoIOSuite, Stryker4sIOSuite}

class ReporterTest extends Stryker4sIOSuite with MockitoIOSuite with LogMatchers {
  describe("reporter") {
    it("should log that the console reporter is used when a non existing reporter is configured") {
      val consoleReporterMock = mock[ConsoleReporter]
      whenF(consoleReporterMock.reportRunFinished(any[FinishedRunReport])).thenReturn(())

      val report = MutationTestReport(thresholds = Thresholds(100, 0), files = Map.empty)
      val metrics = Metrics.calculateMetrics(report)
      val runReport = FinishedRunReport(report, metrics, 10.seconds, File("target/stryker4s-report/"))
      val sut = new AggregateReporter(Seq(consoleReporterMock))

      sut
        .reportRunFinished(runReport)
        .map { _ =>
          verify(consoleReporterMock).reportRunFinished(runReport)
        }
        .assertNoException
    }

    describe("reportMutationStart") {
      it("should report to all progressReporters that a mutation run will start") {
        val eventMock = mock[StartMutationEvent]
        val consoleReporterMock = mock[ConsoleReporter]
        val progressReporterMock = mock[ProgressReporter]
        val sut = new AggregateReporter(Seq(consoleReporterMock, progressReporterMock))
        whenF(consoleReporterMock.reportMutationStart(any[StartMutationEvent])).thenReturn(())
        whenF(progressReporterMock.reportMutationStart(any[StartMutationEvent])).thenReturn(())

        sut
          .reportMutationStart(eventMock)
          .map { _ =>
            verify(consoleReporterMock).reportMutationStart(eventMock)
            verify(progressReporterMock).reportMutationStart(eventMock)
          }
          .assertNoException
      }

      it("should not report to finishedMutationRunReporters that a mutation run will start") {
        val consoleReporterMock = mock[ConsoleReporter]
        val finishedRunReporterMock = mock[FinishedRunReporter]
        val mutantRunResultMock = mock[StartMutationEvent]
        val sut = new AggregateReporter(Seq(consoleReporterMock, finishedRunReporterMock))
        whenF(consoleReporterMock.reportMutationStart(any[StartMutationEvent])).thenReturn(())

        sut
          .reportMutationStart(mutantRunResultMock)
          .map { _ =>
            verify(consoleReporterMock).reportMutationStart(mutantRunResultMock)
            verifyZeroInteractions(finishedRunReporterMock)
          }
          .assertNoException
      }
    }

    describe("reportRunFinished") {
      it("should report to all finished mutation run reporters that a mutation run is completed") {
        val consoleReporterMock = mock[ConsoleReporter]
        val finishedRunReporterMock = mock[FinishedRunReporter]
        whenF(consoleReporterMock.reportRunFinished(any[FinishedRunReport])).thenReturn(())
        whenF(finishedRunReporterMock.reportRunFinished(any[FinishedRunReport])).thenReturn(())
        val report = MutationTestReport(thresholds = Thresholds(100, 0), files = Map.empty)
        val metrics = Metrics.calculateMetrics(report)
        val runReport = FinishedRunReport(report, metrics, 10.seconds, File("target/stryker4s-report/"))
        val sut: AggregateReporter = new AggregateReporter(Seq(consoleReporterMock, finishedRunReporterMock))

        sut
          .reportRunFinished(runReport)
          .map { _ =>
            verify(consoleReporterMock).reportRunFinished(runReport)
            verify(finishedRunReporterMock).reportRunFinished(runReport)
          }
          .assertNoException
      }

      it("should not report a finished mutation run to a progress reporter") {
        val consoleReporterMock = mock[ConsoleReporter]
        val progressReporterMock = mock[ProgressReporter]
        whenF(consoleReporterMock.reportRunFinished(any[FinishedRunReport])).thenReturn(())
        val report = MutationTestReport(thresholds = Thresholds(100, 0), files = Map.empty)
        val metrics = Metrics.calculateMetrics(report)
        val runReport = FinishedRunReport(report, metrics, 10.seconds, File("target/stryker4s-report/"))
        val sut = new AggregateReporter(Seq(consoleReporterMock, progressReporterMock))

        sut
          .reportRunFinished(runReport)
          .map { _ =>
            verify(consoleReporterMock).reportRunFinished(runReport)
            verifyZeroInteractions(progressReporterMock)
          }
          .assertNoException
      }

      it("should still call other reporters if a reporter throws an exception") {
        val consoleReporterMock = mock[ConsoleReporter]
        val progressReporterMock = mock[FinishedRunReporter]
        whenF(progressReporterMock.reportRunFinished(any[FinishedRunReport])).thenReturn(())
        val report = MutationTestReport(thresholds = Thresholds(100, 0), files = Map.empty)
        val metrics = Metrics.calculateMetrics(report)
        val runReport = FinishedRunReport(report, metrics, 10.seconds, File("target/stryker4s-report/"))
        val sut = new AggregateReporter(Seq(consoleReporterMock, progressReporterMock))
        whenF(consoleReporterMock.reportRunFinished(runReport)).thenFailWith(new RuntimeException("Something happened"))

        sut
          .reportRunFinished(runReport)
          .map { _ =>
            verify(progressReporterMock).reportRunFinished(runReport)
          }
          .assertNoException
      }

      describe("logging") {
        val failedToReportMessage = "1 reporter(s) failed to report:"
        val exceptionMessage = "java.lang.RuntimeException: Something happened"

        val progressReporterMock = mock[ProgressReporter]
        val report = MutationTestReport(thresholds = Thresholds(100, 0), files = Map.empty)
        val metrics = Metrics.calculateMetrics(report)
        val runReport = FinishedRunReport(report, metrics, 10.seconds, File("target/stryker4s-report/"))

        it("should log if a report throws an exception") {
          val consoleReporterMock = mock[ConsoleReporter]
          val sut = new AggregateReporter(Seq(consoleReporterMock, progressReporterMock))
          whenF(consoleReporterMock.reportRunFinished(runReport))
            .thenFailWith(new RuntimeException("Something happened"))

          sut
            .reportRunFinished(runReport)
            .map { _ =>
              failedToReportMessage shouldBe loggedAsWarning
              exceptionMessage shouldBe loggedAsWarning
            }
        }

        it("should not log warnings if no exceptions occur") {
          val consoleReporterMock = mock[ConsoleReporter]
          whenF(consoleReporterMock.reportRunFinished(any[FinishedRunReport])).thenReturn(())
          val sut = new AggregateReporter(Seq(consoleReporterMock, progressReporterMock))

          sut
            .reportRunFinished(runReport)
            .map { _ =>
              verify(consoleReporterMock).reportRunFinished(runReport)
              failedToReportMessage should not be loggedAsWarning
              exceptionMessage should not be loggedAsWarning
            }
        }
      }
    }
  }
}
