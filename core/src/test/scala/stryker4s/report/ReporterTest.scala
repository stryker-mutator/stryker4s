package stryker4s.report

import mutationtesting._
import stryker4s.config.Config
import stryker4s.model.{Mutant, MutantRunResult}
import stryker4s.scalatest.LogMatchers
import stryker4s.testutil.{AsyncStryker4sSuite, MockitoSuite}
import stryker4s.extension.mutationtype.GreaterThan
import scala.meta._

class ReporterTest extends AsyncStryker4sSuite with MockitoSuite with LogMatchers {
  describe("reporter") {
    it("should log that the console reporter is used when a non existing reporter is configured") {
      val consoleReporterMock = mock[ConsoleReporter]
      implicit val config: Config = Config.default

      val report = MutationTestReport(thresholds = Thresholds(100, 0), files = Map.empty)
      val metrics = Metrics.calculateMetrics(report)
      val runReport = FinishedRunReport(report, metrics)

      val sut: Reporter = new Reporter() {
        override lazy val reporters: Seq[ConsoleReporter] = Seq(consoleReporterMock)
      }

      sut
        .reportRunFinished(runReport)
        .map { _ =>
          verify(consoleReporterMock).reportRunFinished(runReport)
          succeed
        }
        .unsafeToFuture()
    }

    describe("reportMutationStart") {
      it("should report to all progressReporters that a mutation run is started.") {
        val mutantMock = Mutant(0, q">", q"<", GreaterThan)
        val consoleReporterMock = mock[ConsoleReporter]
        val progressReporterMock = mock[ProgressReporter]

        implicit val config: Config = Config(reporters = Set.empty)

        val sut: Reporter = new Reporter() {
          override lazy val reporters: Seq[MutationRunReporter] = Seq(consoleReporterMock, progressReporterMock)
        }

        sut
          .reportMutationStart(mutantMock)
          .map { _ =>
            verify(consoleReporterMock).reportMutationStart(mutantMock)
            verify(progressReporterMock).reportMutationStart(mutantMock)
            succeed
          }
          .unsafeToFuture()
      }

      it("Should not report to finishedRunReporters that is mutation run is started.") {
        val consoleReporterMock = mock[ConsoleReporter]
        val finishedRunReporterMock = mock[FinishedRunReporter]
        val mutantMock = Mutant(0, q">", q"<", GreaterThan)

        implicit val config: Config = Config.default

        val sut: Reporter = new Reporter() {
          override lazy val reporters: Seq[MutationRunReporter] = Seq(consoleReporterMock, finishedRunReporterMock)
        }

        sut
          .reportMutationStart(mutantMock)
          .map { _ =>
            verify(consoleReporterMock).reportMutationStart(mutantMock)
            verifyZeroInteractions(finishedRunReporterMock)
            succeed
          }
          .unsafeToFuture()
      }
    }

    describe("reportMutationComplete") {
      it("should report to all progressReporters that a mutation run is completed") {
        val mutantRunResultMock = mock[MutantRunResult]
        val consoleReporterMock = mock[ConsoleReporter]
        val progressReporterMock = mock[ProgressReporter]

        implicit val config: Config = Config(reporters = Set.empty)

        val sut: Reporter = new Reporter() {
          override lazy val reporters: Seq[ProgressReporter] = Seq(consoleReporterMock, progressReporterMock)
        }

        sut
          .reportMutationComplete(mutantRunResultMock, 1)
          .map { _ =>
            verify(consoleReporterMock).reportMutationComplete(mutantRunResultMock, 1)
            verify(progressReporterMock).reportMutationComplete(mutantRunResultMock, 1)
            succeed
          }
          .unsafeToFuture()
      }

      it("should not report to finishedMutationRunReporters that a mutation run is completed") {
        val consoleReporterMock = mock[ConsoleReporter]
        val finishedRunReporterMock = mock[FinishedRunReporter]
        val mutantRunResultMock = mock[MutantRunResult]

        implicit val config: Config = Config.default

        val sut: Reporter = new Reporter() {
          override lazy val reporters: Seq[MutationRunReporter] = Seq(consoleReporterMock, finishedRunReporterMock)
        }

        sut
          .reportMutationComplete(mutantRunResultMock, 1)
          .map { _ =>
            verify(consoleReporterMock).reportMutationComplete(mutantRunResultMock, 1)
            verifyZeroInteractions(finishedRunReporterMock)
            succeed
          }
          .unsafeToFuture()
      }
    }

    describe("reportRunFinished") {
      it("should report to all finished mutation run reporters that a mutation run is completed") {
        val consoleReporterMock = mock[ConsoleReporter]
        val finishedRunReporterMock = mock[FinishedRunReporter]
        implicit val config: Config = Config.default

        val report = MutationTestReport(thresholds = Thresholds(100, 0), files = Map.empty)
        val metrics = Metrics.calculateMetrics(report)
        val runReport = FinishedRunReport(report, metrics)

        val sut: Reporter = new Reporter() {
          override lazy val reporters: Seq[MutationRunReporter] = Seq(consoleReporterMock, finishedRunReporterMock)
        }

        sut
          .reportRunFinished(runReport)
          .map { _ =>
            verify(consoleReporterMock).reportRunFinished(runReport)
            verify(finishedRunReporterMock).reportRunFinished(runReport)
            succeed
          }
          .unsafeToFuture()
      }

      it("should not report a finished mutation run to a progress reporter") {
        val consoleReporterMock = mock[ConsoleReporter]
        val progressReporterMock = mock[ProgressReporter]
        implicit val config: Config = Config.default

        val report = MutationTestReport(thresholds = Thresholds(100, 0), files = Map.empty)
        val metrics = Metrics.calculateMetrics(report)
        val runReport = FinishedRunReport(report, metrics)

        val sut: Reporter = new Reporter() {
          override lazy val reporters: Seq[MutationRunReporter] = Seq(consoleReporterMock, progressReporterMock)
        }

        sut
          .reportRunFinished(runReport)
          .map { _ =>
            verify(consoleReporterMock).reportRunFinished(runReport)
            verifyZeroInteractions(progressReporterMock)
            succeed
          }
          .unsafeToFuture()
      }

      it("should still call other reporters if a reporter throws an exception") {
        val consoleReporterMock = mock[ConsoleReporter]
        val progressReporterMock = mock[FinishedRunReporter]
        implicit val config: Config = Config.default

        val report = MutationTestReport(thresholds = Thresholds(100, 0), files = Map.empty)
        val metrics = Metrics.calculateMetrics(report)
        val runReport = FinishedRunReport(report, metrics)
        when(consoleReporterMock.reportRunFinished(runReport))
          .thenThrow(new RuntimeException("Something happened"))

        val sut: Reporter = new Reporter() {
          override lazy val reporters: Seq[MutationRunReporter] = Seq(consoleReporterMock, progressReporterMock)
        }

        sut
          .reportRunFinished(runReport)
          .map { _ =>
            verify(progressReporterMock).reportRunFinished(runReport)
            succeed
          }
          .unsafeToFuture()
      }

      describe("logging") {
        val failedToReportMessage = "1 reporter(s) failed to report:"
        val exceptionMessage = "java.lang.RuntimeException: Something happened"

        val progressReporterMock = mock[ProgressReporter]
        implicit val config: Config = Config.default

        val report = MutationTestReport(thresholds = Thresholds(100, 0), files = Map.empty)
        val metrics = Metrics.calculateMetrics(report)
        val runReport = FinishedRunReport(report, metrics)

        it("should log if a report throws an exception") {
          val consoleReporterMock = mock[ConsoleReporter]
          val sut: Reporter = new Reporter() {
            override lazy val reporters: Seq[MutationRunReporter] = Seq(consoleReporterMock, progressReporterMock)
          }
          when(consoleReporterMock.reportRunFinished(runReport))
            .thenThrow(new RuntimeException("Something happened"))

          sut
            .reportRunFinished(runReport)
            .map { _ =>
              failedToReportMessage shouldBe loggedAsWarning
              exceptionMessage shouldBe loggedAsWarning
            }
            .unsafeToFuture()
        }

        it("should not log warnings if no exceptions occur") {
          val consoleReporterMock = mock[ConsoleReporter]
          val sut: Reporter = new Reporter() {
            override lazy val reporters: Seq[MutationRunReporter] = Seq(consoleReporterMock, progressReporterMock)
          }

          sut
            .reportRunFinished(runReport)
            .map { _ =>
              verify(consoleReporterMock).reportRunFinished(runReport)
              failedToReportMessage should not be loggedAsWarning
              exceptionMessage should not be loggedAsWarning
            }
            .unsafeToFuture()
        }
      }
    }
  }
}
