package stryker4s.report

import org.mockito.integrations.scalatest.MockitoFixture
import stryker4s.config.Config
import stryker4s.model.{Mutant, MutantRunResult, MutantRunResults}
import stryker4s.testutil.Stryker4sSuite

import scala.concurrent.duration._
import scala.language.postfixOps

class ReporterTest extends Stryker4sSuite with MockitoFixture {

  describe("reporter") {

    it("should log that the console reporter is used when a non existing reporter is configured") {
      val consoleReporterMock = mock[ConsoleReporter]
      implicit val config: Config = Config()

      val mutantRunResults = MutantRunResults(List.empty, 100.0, 10 seconds)

      val sut: Reporter = new Reporter() {
        override def reporters: Seq[ConsoleReporter] = Seq(consoleReporterMock)
      }

      sut.reportRunFinished(mutantRunResults)

      verify(consoleReporterMock).reportRunFinished(mutantRunResults)
    }

    describe("reportMutationStart") {
      it("should report to all progressReporters that a mutation run is started.") {
        val mutantMock = mock[Mutant]
        val consoleReporterMock = mock[ConsoleReporter]
        val progressReporterMock = mock[ProgressionReporter]

        implicit val config: Config = Config(reporters = Seq())

        val sut: Reporter = new Reporter() {
          override def reporters: Seq[ProgressReporter] = Seq(consoleReporterMock, progressReporterMock)
        }

        sut.reportMutationStart(mutantMock)

        verify(consoleReporterMock).reportMutationStart(mutantMock)
        verify(progressReporterMock).reportMutationStart(mutantMock)
      }

      it("Should not report to finishedRunReporters that is mutation run is started.") {
        val consoleReporterMock = mock[ConsoleReporter]
        val finishedRunReporterMock = mock[FinishedReporter]
        val mutantMock = mock[Mutant]

        implicit val config: Config = Config()

        val sut: Reporter = new Reporter() {
          override def reporters: Seq[MutationRunReporter] = Seq(consoleReporterMock, finishedRunReporterMock)
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
        val progressReporterMock = mock[ProgressionReporter]

        implicit val config: Config = Config(reporters = Seq())

        val sut: Reporter = new Reporter() {
          override def reporters: Seq[ProgressReporter] = Seq(consoleReporterMock, progressReporterMock)
        }

        sut.reportMutationComplete(mutantRunResultMock, 1)

        verify(consoleReporterMock).reportMutationComplete(mutantRunResultMock, 1)
        verify(progressReporterMock).reportMutationComplete(mutantRunResultMock, 1)
      }

      it("should not report to finishedMutationRunReporters that a mutation run is completed") {
        val consoleReporterMock = mock[ConsoleReporter]
        val finishedRunReporterMock = mock[FinishedReporter]
        val mutantRunResultMock = mock[MutantRunResult]

        implicit val config: Config = Config()

        val sut: Reporter = new Reporter() {
          override def reporters: Seq[MutationRunReporter] = Seq(consoleReporterMock, finishedRunReporterMock)
        }

        sut.reportMutationComplete(mutantRunResultMock, 1)

        verify(consoleReporterMock).reportMutationComplete(mutantRunResultMock, 1)
        verifyZeroInteractions(finishedRunReporterMock)
      }
    }

    describe("reportRunFinished") {
      it("should report to all finished mutation run reporters that a mutation run is completed") {
        val consoleReporterMock = mock[ConsoleReporter]
        val finishedReporterMock = mock[FinishedReporter]
        implicit val config: Config = Config()

        val mutantRunResults = MutantRunResults(List.empty, 100.0, 10 seconds)

        val sut: Reporter = new Reporter() {
          override def reporters: Seq[MutationRunReporter] = Seq(consoleReporterMock, finishedReporterMock)
        }

        sut.reportRunFinished(mutantRunResults)

        verify(consoleReporterMock).reportRunFinished(mutantRunResults)
        verify(finishedReporterMock).reportRunFinished(mutantRunResults)
      }

      it("should not report a finished mutation run to a progress reporter") {
        val consoleReporterMock = mock[ConsoleReporter]
        val progressReporterMock = mock[ProgressionReporter]
        implicit val config: Config = Config()

        val mutantRunResults = MutantRunResults(List.empty, 100.0, 10 seconds)

        val sut: Reporter = new Reporter() {
          override def reporters: Seq[MutationRunReporter] = Seq(consoleReporterMock, progressReporterMock)
        }

        sut.reportRunFinished(mutantRunResults)

        verify(consoleReporterMock).reportRunFinished(mutantRunResults)
        verifyZeroInteractions(progressReporterMock)
      }
    }
  }

  class ProgressionReporter extends ProgressReporter {
    override def reportMutationStart(mutant: Mutant): Unit = {}
    override def reportMutationComplete(result: MutantRunResult, totalMutants: Int): Unit = {}
  }

  class FinishedReporter extends FinishedRunReporter {
    override def reportRunFinished(runResults: MutantRunResults): Unit = {}
  }
}
