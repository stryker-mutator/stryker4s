package stryker4s.report

import mutationtesting._
import stryker4s.config.Config
import stryker4s.model.{Mutant, MutantRunResult}
import stryker4s.scalatest.LogMatchers
import stryker4s.testutil.{MockitoSuite, Stryker4sSuite}

class ReporterTest extends Stryker4sSuite with MockitoSuite with LogMatchers {
  describe("reporter") {
    it("should log that the console reporter is used when a non existing reporter is configured") {
      val consoleReporterMock = mock[ConsoleReporter]
      implicit val config: Config = Config.default

      val report = MutationTestReport(thresholds = Thresholds(100, 0), files = Map.empty)
      val metrics = Metrics.calculateMetrics(report)

      val sut: Reporter = new Reporter() {
        override lazy val reporters: Seq[ConsoleReporter] = Seq(consoleReporterMock)
      }

      sut.reportRunFinished(report, metrics)

      verify(consoleReporterMock).reportRunFinished(report, metrics)
    }

    describe("reportMutationStart") {
      it("should report to all progressReporters that a mutation run is started.") {
        val mutantMock = mock[Mutant]
        val consoleReporterMock = mock[ConsoleReporter]
        val progressReporterMock = mock[ProgressReporter]

        implicit val config: Config = Config(reporters = Seq())

        val sut: Reporter = new Reporter() {
          override lazy val reporters: Seq[MutationRunReporter] = Seq(consoleReporterMock, progressReporterMock)
        }

        sut.reportMutationStart(mutantMock)

        verify(consoleReporterMock).reportMutationStart(mutantMock)
        verify(progressReporterMock).reportMutationStart(mutantMock)
      }

      it("Should not report to finishedRunReporters that is mutation run is started.") {
        val consoleReporterMock = mock[ConsoleReporter]
        val finishedRunReporterMock = mock[FinishedRunReporter]
        val mutantMock = mock[Mutant]

        implicit val config: Config = Config.default

        val sut: Reporter = new Reporter() {
          override lazy val reporters: Seq[MutationRunReporter] = Seq(consoleReporterMock, finishedRunReporterMock)
        }

        sut.reportMutationStart(mutantMock)

        verify(consoleReporterMock).reportMutationStart(mutantMock)
        verifyZeroInteractions(finishedRunReporterMock)
      }
    }

    describe("reportMutationComplete") {
      it("should report to all progressReporters that a mutation run is completed") {
        val mutantRunResultMock = mock[MutantRunResult]
        val consoleReporterMock = mock[ConsoleReporter]
        val progressReporterMock = mock[ProgressReporter]

        implicit val config: Config = Config(reporters = Seq())

        val sut: Reporter = new Reporter() {
          override lazy val reporters: Seq[ProgressReporter] = Seq(consoleReporterMock, progressReporterMock)
        }

        sut.reportMutationComplete(mutantRunResultMock, 1)

        verify(consoleReporterMock).reportMutationComplete(mutantRunResultMock, 1)
        verify(progressReporterMock).reportMutationComplete(mutantRunResultMock, 1)
      }

      it("should not report to finishedMutationRunReporters that a mutation run is completed") {
        val consoleReporterMock = mock[ConsoleReporter]
        val finishedRunReporterMock = mock[FinishedRunReporter]
        val mutantRunResultMock = mock[MutantRunResult]

        implicit val config: Config = Config.default

        val sut: Reporter = new Reporter() {
          override lazy val reporters: Seq[MutationRunReporter] = Seq(consoleReporterMock, finishedRunReporterMock)
        }

        sut.reportMutationComplete(mutantRunResultMock, 1)

        verify(consoleReporterMock).reportMutationComplete(mutantRunResultMock, 1)
        verifyZeroInteractions(finishedRunReporterMock)
      }
    }

    describe("reportRunFinished") {
      it("should report to all finished mutation run reporters that a mutation run is completed") {
        val consoleReporterMock = mock[ConsoleReporter]
        val finishedRunReporterMock = mock[FinishedRunReporter]
        implicit val config: Config = Config.default

        val report = MutationTestReport(thresholds = Thresholds(100, 0), files = Map.empty)
        val metrics = Metrics.calculateMetrics(report)

        val sut: Reporter = new Reporter() {
          override lazy val reporters: Seq[MutationRunReporter] = Seq(consoleReporterMock, finishedRunReporterMock)
        }

        sut.reportRunFinished(report, metrics)

        verify(consoleReporterMock).reportRunFinished(report, metrics)
        verify(finishedRunReporterMock).reportRunFinished(report, metrics)
      }

      it("should not report a finished mutation run to a progress reporter") {
        val consoleReporterMock = mock[ConsoleReporter]
        val progressReporterMock = mock[ProgressReporter]
        implicit val config: Config = Config.default

        val report = MutationTestReport(thresholds = Thresholds(100, 0), files = Map.empty)
        val metrics = Metrics.calculateMetrics(report)

        val sut: Reporter = new Reporter() {
          override lazy val reporters: Seq[MutationRunReporter] = Seq(consoleReporterMock, progressReporterMock)
        }

        sut.reportRunFinished(report, metrics)

        verify(consoleReporterMock).reportRunFinished(report, metrics)
        verifyZeroInteractions(progressReporterMock)
      }

      it("should still call other reporters if a reporter throws an exception") {
        val consoleReporterMock = mock[ConsoleReporter]
        val progressReporterMock = mock[FinishedRunReporter]
        implicit val config: Config = Config.default

        val report = MutationTestReport(thresholds = Thresholds(100, 0), files = Map.empty)
        val metrics = Metrics.calculateMetrics(report)
        when(consoleReporterMock.reportRunFinished(report, metrics))
          .thenThrow(new RuntimeException("Something happened"))

        val sut: Reporter = new Reporter() {
          override lazy val reporters: Seq[MutationRunReporter] = Seq(consoleReporterMock, progressReporterMock)
        }

        sut.reportRunFinished(report, metrics)

        verify(progressReporterMock).reportRunFinished(report, metrics)
      }

      describe("logging") {
        val failedToReportMessage = "1 reporter(s) failed to report:"
        val exceptionMessage = "java.lang.RuntimeException: Something happened"

        val progressReporterMock = mock[ProgressReporter]
        implicit val config: Config = Config.default

        val report = MutationTestReport(thresholds = Thresholds(100, 0), files = Map.empty)
        val metrics = Metrics.calculateMetrics(report)

        it("should log if a report throws an exception") {
          val consoleReporterMock = mock[ConsoleReporter]
          val sut: Reporter = new Reporter() {
            override lazy val reporters: Seq[MutationRunReporter] = Seq(consoleReporterMock, progressReporterMock)
          }
          when(consoleReporterMock.reportRunFinished(report, metrics))
            .thenThrow(new RuntimeException("Something happened"))

          sut.reportRunFinished(report, metrics)

          failedToReportMessage shouldBe loggedAsWarning
          exceptionMessage shouldBe loggedAsWarning
        }

        it("should not log warnings if no exceptions occur") {
          val consoleReporterMock = mock[ConsoleReporter]
          val sut: Reporter = new Reporter() {
            override lazy val reporters: Seq[MutationRunReporter] = Seq(consoleReporterMock, progressReporterMock)
          }

          sut.reportRunFinished(report, metrics)

          verify(consoleReporterMock).reportRunFinished(report, metrics)
          failedToReportMessage should not be loggedAsWarning
          exceptionMessage should not be loggedAsWarning
        }
      }
    }
  }
}
