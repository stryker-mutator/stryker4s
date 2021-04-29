package stryker4s.report

import better.files.File
import cats.data.NonEmptyList
import fs2.{CompositeFailure, Stream}
import mutationtesting._
import stryker4s.model.NoCoverage
import stryker4s.scalatest.LogMatchers
import stryker4s.testutil.{MockitoIOSuite, Stryker4sIOSuite, TestData}

import java.nio.file.Paths
import scala.concurrent.duration._

class AggregateReporterTest extends Stryker4sIOSuite with MockitoIOSuite with LogMatchers with TestData {

  override def printLogs = true
  describe("mutantPlaced") {
    it("should do nothing if there are no reporters") {
      val sut = new AggregateReporter(Seq.empty)

      Stream(createMutant)
        .through(sut.mutantPlaced)
        .compile
        .toVector
        .asserting(_ shouldBe empty)
    }
  }

  describe("mutantTested") {
    it("should do nothing if there are no reporters") {
      val sut = new AggregateReporter(Seq.empty)

      Stream((Paths.get("."), NoCoverage(createMutant)))
        .through(sut.mutantTested)
        .compile
        .toVector
        .asserting(_ shouldBe empty)
    }
  }

  describe("onRunFinished") {
    val runReport = createFinishedRunEvent()

    it("should do nothing if there are no reporters") {
      val sut = new AggregateReporter(Seq.empty)

      Stream(createMutant)
        .through(sut.mutantPlaced)
        .compile
        .toVector
        .asserting(_ shouldBe empty)
    }

    it("should report to all finished mutation run reporters that a mutation run is completed") {
      val reporter1 = mock[Reporter]
      val reporter2 = mock[Reporter]
      whenF(reporter1.onRunFinished(any[FinishedRunEvent])).thenReturn(())
      whenF(reporter2.onRunFinished(any[FinishedRunEvent])).thenReturn(())
      val sut: AggregateReporter = new AggregateReporter(Seq(reporter1, reporter2))

      sut
        .onRunFinished(runReport)
        .map { _ =>
          verify(reporter1).onRunFinished(runReport)
          verify(reporter2).onRunFinished(runReport)
        }
        .assertNoException
    }

    it("should still call other reporters if a reporter throws an exception") {
      val failingReporter = mock[Reporter]
      val reporter2 = mock[Reporter]
      whenF(failingReporter.onRunFinished(runReport)).thenFailWith(new RuntimeException("Something happened"))
      whenF(reporter2.onRunFinished(any[FinishedRunEvent])).thenReturn(())
      val sut = new AggregateReporter(Seq(failingReporter, reporter2))

      sut
        .onRunFinished(runReport)
        .map { _ =>
          verify(reporter2).onRunFinished(runReport)
        }
        .assertThrows[RuntimeException]
    }

    describe("logging") {
      val reporter1 = mock[Reporter]
      whenF(reporter1.onRunFinished(any[FinishedRunEvent])).thenReturn(())

      it("should log and throw if a reporter throws an exception") {
        val failingReporter = mock[ConsoleReporter]
        val sut = new AggregateReporter(Seq(failingReporter, reporter1))
        whenF(failingReporter.onRunFinished(runReport))
          .thenFailWith(new RuntimeException("Something happened"))

        sut
          .onRunFinished(runReport)
          .attempt
          .asserting {
            case Left(e) =>
              "Reporter failed to report, java.lang.RuntimeException: Something happened" shouldBe loggedAsError
              e shouldBe a[RuntimeException]
              e.getMessage() shouldBe "Something happened"
            case r => fail(s"Expected exception, got $r")
          }
      }

      it("should exceptions for each failing reporter") {
        val failingReporter1 = mock[ConsoleReporter]
        val failingReporter2 = mock[DashboardReporter]
        val sut = new AggregateReporter(Seq(failingReporter1, failingReporter2))
        val firstExc = new RuntimeException("Something happened")
        val secondExc = new IllegalArgumentException("Something also happened")
        whenF(failingReporter1.onRunFinished(runReport))
          .thenFailWith(firstExc)
        whenF(failingReporter2.onRunFinished(runReport))
          .thenFailWith(secondExc)

        sut
          .onRunFinished(runReport)
          .attempt
          .asserting {
            case Left(e: CompositeFailure) =>
              "Reporter failed to report, fs2.CompositeFailure: Multiple exceptions were thrown (2), first java.lang.RuntimeException: Something happened" shouldBe loggedAsError
              e.all shouldBe NonEmptyList(firstExc, secondExc :: Nil)
            case r => fail(s"Expected exception, got $r")
          }
      }

      it("should not log warnings if no exceptions occur") {
        val consoleReporterMock = mock[ConsoleReporter]
        whenF(consoleReporterMock.onRunFinished(any[FinishedRunEvent])).thenReturn(())
        val sut = new AggregateReporter(Seq(consoleReporterMock, reporter1))

        sut
          .onRunFinished(runReport)
          .asserting { _ =>
            verify(consoleReporterMock).onRunFinished(runReport)
            "Reporter failed to report" should not be loggedAsWarning
          }
      }
    }
  }

  // describe("reporter") {
  //   it("should log that the console reporter is used when a non existing reporter is configured") {
  //     val consoleReporterMock = mock[ConsoleReporter]
  //     whenF(consoleReporterMock.onRunFinished(any[FinishedRunEvent])).thenReturn(())

  //     val report = MutationTestResult(thresholds = Thresholds(100, 0), files = Map.empty)
  //     val metrics = Metrics.calculateMetrics(report)
  //     val runReport = FinishedRunEvent(report, metrics, 10.seconds, File("target/stryker4s-report/"))
  //     val sut = new AggregateReporter(Seq(consoleReporterMock))

  //     sut
  //       .onRunFinished(runReport)
  //       .map { _ =>
  //         verify(consoleReporterMock).onRunFinished(runReport)
  //       }
  //       .assertNoException
  //   }

  //   describe("onMutationStart") {
  //     it("should report to all progressReporters that a mutation run will start") {
  //       val eventMock = mock[StartMutationEvent]
  //       val consoleReporterMock = mock[ConsoleReporter]
  //       val progressReporterMock = mock[ProgressReporter]
  //       val sut = new AggregateReporter(Seq(consoleReporterMock, progressReporterMock))
  //       whenF(consoleReporterMock.onMutationStart(any[StartMutationEvent])).thenReturn(())
  //       whenF(progressReporterMock.onMutationStart(any[StartMutationEvent])).thenReturn(())

  //       sut
  //         .onMutationStart(eventMock)
  //         .map { _ =>
  //           verify(consoleReporterMock).onMutationStart(eventMock)
  //           verify(progressReporterMock).onMutationStart(eventMock)
  //         }
  //         .assertNoException
  //     }

  //     it("should not report to finishedMutationRunReporters that a mutation run will start") {
  //       val consoleReporterMock = mock[ConsoleReporter]
  //       val finishedRunReporterMock = mock[FinishedRunReporter]
  //       val mutantRunResultMock = mock[StartMutationEvent]
  //       val sut = new AggregateReporter(Seq(consoleReporterMock, finishedRunReporterMock))
  //       whenF(consoleReporterMock.onMutationStart(any[StartMutationEvent])).thenReturn(())

  //       sut
  //         .onMutationStart(mutantRunResultMock)
  //         .map { _ =>
  //           verify(consoleReporterMock).onMutationStart(mutantRunResultMock)
  //           verifyZeroInteractions(finishedRunReporterMock)
  //         }
  //         .assertNoException
  //     }

  //     describe("logging") {
  //       it("should log and continue if one  reporter throws an exception") {
  //         val eventMock = mock[StartMutationEvent]
  //         val consoleReporterMock = mock[ConsoleReporter]
  //         val progressReporterMock = mock[ProgressReporter]
  //         val sut = new AggregateReporter(Seq(consoleReporterMock, progressReporterMock))
  //         val e = new RuntimeException("Something happened")
  //         whenF(consoleReporterMock.onMutationStart(any[StartMutationEvent]))
  //           .thenFailWith(e)
  //         whenF(progressReporterMock.onMutationStart(any[StartMutationEvent])).thenReturn(())

  //         sut
  //           .onMutationStart(eventMock)
  //           .map { _ =>
  //             "1 reporter failed to report:" shouldBe loggedAsError
  //             e.toString() shouldBe loggedAsError
  //             verify(consoleReporterMock).onMutationStart(eventMock)
  //             verify(progressReporterMock).onMutationStart(eventMock)
  //           }
  //           .assertNoException
  //       }
  //     }
  //   }

  //   describe("onRunFinished") {
  //     it("should report to all finished mutation run reporters that a mutation run is completed") {
  //       val consoleReporterMock = mock[ConsoleReporter]
  //       val finishedRunReporterMock = mock[FinishedRunReporter]
  //       whenF(consoleReporterMock.onRunFinished(any[FinishedRunEvent])).thenReturn(())
  //       whenF(finishedRunReporterMock.onRunFinished(any[FinishedRunEvent])).thenReturn(())
  //       val report = MutationTestResult(thresholds = Thresholds(100, 0), files = Map.empty)
  //       val metrics = Metrics.calculateMetrics(report)
  //       val runReport = FinishedRunEvent(report, metrics, 10.seconds, File("target/stryker4s-report/"))
  //       val sut: AggregateReporter = new AggregateReporter(Seq(consoleReporterMock, finishedRunReporterMock))

  //       sut
  //         .onRunFinished(runReport)
  //         .map { _ =>
  //           verify(consoleReporterMock).onRunFinished(runReport)
  //           verify(finishedRunReporterMock).onRunFinished(runReport)
  //         }
  //         .assertNoException
  //     }

  //     it("should not report a finished mutation run to a progress reporter") {
  //       val consoleReporterMock = mock[ConsoleReporter]
  //       val progressReporterMock = mock[ProgressReporter]
  //       whenF(consoleReporterMock.onRunFinished(any[FinishedRunEvent])).thenReturn(())
  //       val report = MutationTestResult(thresholds = Thresholds(100, 0), files = Map.empty)
  //       val metrics = Metrics.calculateMetrics(report)
  //       val runReport = FinishedRunEvent(report, metrics, 10.seconds, File("target/stryker4s-report/"))
  //       val sut = new AggregateReporter(Seq(consoleReporterMock, progressReporterMock))

  //       sut
  //         .onRunFinished(runReport)
  //         .map { _ =>
  //           verify(consoleReporterMock).onRunFinished(runReport)
  //           verifyZeroInteractions(progressReporterMock)
  //         }
  //         .assertNoException
  //     }

  //     it("should still call other reporters if a reporter throws an exception") {
  //       val consoleReporterMock = mock[ConsoleReporter]
  //       val progressReporterMock = mock[FinishedRunReporter]
  //       whenF(progressReporterMock.onRunFinished(any[FinishedRunEvent])).thenReturn(())
  //       val report = MutationTestResult(thresholds = Thresholds(100, 0), files = Map.empty)
  //       val metrics = Metrics.calculateMetrics(report)
  //       val runReport = FinishedRunEvent(report, metrics, 10.seconds, File("target/stryker4s-report/"))
  //       val sut = new AggregateReporter(Seq(consoleReporterMock, progressReporterMock))
  //       whenF(consoleReporterMock.onRunFinished(runReport)).thenFailWith(new RuntimeException("Something happened"))

  //       sut
  //         .onRunFinished(runReport)
  //         .map { _ =>
  //           verify(progressReporterMock).onRunFinished(runReport)
  //         }
  //         .assertThrows[RuntimeException]
  //     }

  // }
  // }
}
