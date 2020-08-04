package stryker4s.report

import mutationtesting._
import stryker4s.model.{Mutant, MutantRunResult}
import stryker4s.scalatest.LogMatchers
import stryker4s.testutil.{MockitoSuite, Stryker4sSuite}
import stryker4s.extension.mutationtype.GreaterThan
import scala.meta._
import cats.effect.IO

class ReporterTest extends Stryker4sSuite with MockitoSuite with LogMatchers {
  describe("reporter") {
    it("should log that the console reporter is used when a non existing reporter is configured") {
      val consoleReporterMock = mock[ConsoleReporter]
      when(consoleReporterMock.reportRunFinished(any[FinishedRunReport])).thenReturn(IO.unit)

      val report = MutationTestReport(thresholds = Thresholds(100, 0), files = Map.empty)
      val metrics = Metrics.calculateMetrics(report)
      val runReport = FinishedRunReport(report, metrics)
      val sut = new AggregateReporter(Seq(consoleReporterMock))

      sut
        .reportRunFinished(runReport)
        .unsafeRunSync()

      verify(consoleReporterMock).reportRunFinished(runReport)
    }

    describe("reportMutationStart") {
      it("should report to all progressReporters that a mutation run is started.") {
        val mutantMock = Mutant(0, q">", q"<", GreaterThan)
        val consoleReporterMock = mock[ConsoleReporter]
        val progressReporterMock = mock[ProgressReporter]
        when(consoleReporterMock.reportMutationStart(any[Mutant])).thenReturn(IO.unit)
        when(progressReporterMock.reportMutationStart(any[Mutant])).thenReturn(IO.unit)
        val sut = new AggregateReporter(Seq(consoleReporterMock, progressReporterMock))
        sut
          .reportMutationStart(mutantMock)
          .unsafeRunSync()

        verify(consoleReporterMock).reportMutationStart(mutantMock)
        verify(progressReporterMock).reportMutationStart(mutantMock)
      }

      it("Should not report to finishedRunReporters that is mutation run is started.") {
        val consoleReporterMock = mock[ConsoleReporter]
        when(consoleReporterMock.reportMutationStart(any[Mutant])).thenReturn(IO.unit)
        val finishedRunReporterMock = mock[FinishedRunReporter]
        val mutantMock = Mutant(0, q">", q"<", GreaterThan)
        val sut = new AggregateReporter(Seq(consoleReporterMock, finishedRunReporterMock))
        sut
          .reportMutationStart(mutantMock)
          .unsafeRunSync()

        verify(consoleReporterMock).reportMutationStart(mutantMock)
        verifyZeroInteractions(finishedRunReporterMock)
      }
    }

    describe("reportMutationComplete") {
      it("should report to all progressReporters that a mutation run is completed") {
        val mutantRunResultMock = mock[MutantRunResult]
        val consoleReporterMock = mock[ConsoleReporter]
        val progressReporterMock = mock[ProgressReporter]
        val sut = new AggregateReporter(Seq(consoleReporterMock, progressReporterMock))
        when(consoleReporterMock.reportMutationComplete(any[MutantRunResult], anyInt)).thenReturn(IO.unit)
        when(progressReporterMock.reportMutationComplete(any[MutantRunResult], anyInt)).thenReturn(IO.unit)

        sut
          .reportMutationComplete(mutantRunResultMock, 1)
          .unsafeRunSync()

        verify(consoleReporterMock).reportMutationComplete(mutantRunResultMock, 1)
        verify(progressReporterMock).reportMutationComplete(mutantRunResultMock, 1)
      }

      it("should not report to finishedMutationRunReporters that a mutation run is completed") {
        val consoleReporterMock = mock[ConsoleReporter]
        val finishedRunReporterMock = mock[FinishedRunReporter]
        val mutantRunResultMock = mock[MutantRunResult]
        val sut = new AggregateReporter(Seq(consoleReporterMock, finishedRunReporterMock))
        when(consoleReporterMock.reportMutationComplete(any[MutantRunResult], anyInt)).thenReturn(IO.unit)

        sut
          .reportMutationComplete(mutantRunResultMock, 1)
          .unsafeRunSync()

        verify(consoleReporterMock).reportMutationComplete(mutantRunResultMock, 1)
        verifyZeroInteractions(finishedRunReporterMock)
      }
    }

    describe("reportRunFinished") {
      it("should report to all finished mutation run reporters that a mutation run is completed") {
        val consoleReporterMock = mock[ConsoleReporter]
        val finishedRunReporterMock = mock[FinishedRunReporter]
        when(consoleReporterMock.reportRunFinished(any[FinishedRunReport])).thenReturn(IO.unit)
        when(finishedRunReporterMock.reportRunFinished(any[FinishedRunReport])).thenReturn(IO.unit)
        val report = MutationTestReport(thresholds = Thresholds(100, 0), files = Map.empty)
        val metrics = Metrics.calculateMetrics(report)
        val runReport = FinishedRunReport(report, metrics)
        val sut: AggregateReporter = new AggregateReporter(Seq(consoleReporterMock, finishedRunReporterMock))

        sut
          .reportRunFinished(runReport)
          .unsafeRunSync()

        verify(consoleReporterMock).reportRunFinished(runReport)
        verify(finishedRunReporterMock).reportRunFinished(runReport)
      }

      it("should not report a finished mutation run to a progress reporter") {
        val consoleReporterMock = mock[ConsoleReporter]
        val progressReporterMock = mock[ProgressReporter]
        when(consoleReporterMock.reportRunFinished(any[FinishedRunReport])).thenReturn(IO.unit)
        val report = MutationTestReport(thresholds = Thresholds(100, 0), files = Map.empty)
        val metrics = Metrics.calculateMetrics(report)
        val runReport = FinishedRunReport(report, metrics)
        val sut = new AggregateReporter(Seq(consoleReporterMock, progressReporterMock))

        sut
          .reportRunFinished(runReport)
          .unsafeRunSync()

        verify(consoleReporterMock).reportRunFinished(runReport)
        verifyZeroInteractions(progressReporterMock)
      }

      it("should still call other reporters if a reporter throws an exception") {
        val consoleReporterMock = mock[ConsoleReporter]
        val progressReporterMock = mock[FinishedRunReporter]
        when(progressReporterMock.reportRunFinished(any[FinishedRunReport])).thenReturn(IO.unit)
        val report = MutationTestReport(thresholds = Thresholds(100, 0), files = Map.empty)
        val metrics = Metrics.calculateMetrics(report)
        val runReport = FinishedRunReport(report, metrics)
        val sut = new AggregateReporter(Seq(consoleReporterMock, progressReporterMock))
        when(consoleReporterMock.reportRunFinished(runReport))
          .thenReturn(IO.raiseError(new RuntimeException("Something happened")))

        sut
          .reportRunFinished(runReport)
          .unsafeRunSync()

        verify(progressReporterMock).reportRunFinished(runReport)
      }

      describe("logging") {
        val failedToReportMessage = "1 reporter(s) failed to report:"
        val exceptionMessage = "java.lang.RuntimeException: Something happened"

        val progressReporterMock = mock[ProgressReporter]
        val report = MutationTestReport(thresholds = Thresholds(100, 0), files = Map.empty)
        val metrics = Metrics.calculateMetrics(report)
        val runReport = FinishedRunReport(report, metrics)

        it("should log if a report throws an exception") {
          val consoleReporterMock = mock[ConsoleReporter]
          val sut = new AggregateReporter(Seq(consoleReporterMock, progressReporterMock))
          when(consoleReporterMock.reportRunFinished(runReport))
            .thenReturn(IO.raiseError(new RuntimeException("Something happened")))

          sut
            .reportRunFinished(runReport)
            .unsafeRunSync()

          failedToReportMessage shouldBe loggedAsWarning
          exceptionMessage shouldBe loggedAsWarning
        }

        it("should not log warnings if no exceptions occur") {
          val consoleReporterMock = mock[ConsoleReporter]
          when(consoleReporterMock.reportRunFinished(any[FinishedRunReport])).thenReturn(IO.unit)
          val sut = new AggregateReporter(Seq(consoleReporterMock, progressReporterMock))

          sut
            .reportRunFinished(runReport)
            .unsafeRunSync()

          verify(consoleReporterMock).reportRunFinished(runReport)
          failedToReportMessage should not be loggedAsWarning
          exceptionMessage should not be loggedAsWarning
        }
      }
    }
  }
}
