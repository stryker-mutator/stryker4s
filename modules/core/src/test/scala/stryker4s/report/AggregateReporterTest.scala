package stryker4s.report

import cats.effect.IO
import cats.syntax.all.*
import fs2.Stream
import stryker4s.testkit.{LogMatchers, Stryker4sIOSuite}
import stryker4s.testutil.TestData
import stryker4s.testutil.stubs.ReporterStub

class AggregateReporterTest extends Stryker4sIOSuite with LogMatchers with TestData {

  describe("mutantTested") {
    test("should do nothing if there are no reporters") {
      val sut = new AggregateReporter(List.empty)

      Stream(MutantTestedEvent(1), MutantTestedEvent(2))
        .through(sut.mutantTested)
        .compile
        .count
        .assertEquals(0L)
    }

    test("should report to all reporters that a mutant is tested") {
      val reporter1 = ReporterStub()
      val reporter2 = ReporterStub()
      val sut = new AggregateReporter(List(reporter1, reporter2))

      val events = Stream(MutantTestedEvent(1), MutantTestedEvent(2))
      events
        .through(sut.mutantTested)
        .compile
        .drain >>
        reporter1.mutantTestedCalls.assertEquals(events.toList) *>
        reporter2.mutantTestedCalls.assertEquals(events.toList)
    }

    test("should report to all reporters even if a first reporter fails") {
      val reporter1 = ReporterStub()
      val failingReporter = ReporterStub.throwsException(new RuntimeException("Something happened"))
      val sut = new AggregateReporter(List(failingReporter, reporter1))

      Stream(MutantTestedEvent(1), MutantTestedEvent(2))
        .through(sut.mutantTested)
        .compile
        .drain *> IO.cede >> {
        assertLoggedError("Reporter failed to report, java.lang.RuntimeException: Something happened")
        reporter1.mutantTestedCalls.assert(_.length == 2)
      }
    }
  }

  describe("onRunFinished") {
    val runReport = createFinishedRunEvent()

    test("should report to all reporters that a mutation run is completed") {
      val reporter1 = ReporterStub()
      val reporter2 = ReporterStub()
      val sut: AggregateReporter = new AggregateReporter(List(reporter1, reporter2))

      sut
        .onRunFinished(runReport) >>
        reporter1.onRunFinishedCalls.assert(_.length == 1) *>
        reporter2.onRunFinishedCalls.assert(_.length == 1)

    }

    test("should still call other reporters if a reporter throws an exception") {
      val failingReporter = ReporterStub.throwsException(new RuntimeException("Something happened"))
      val reporter2 = ReporterStub()
      val sut = new AggregateReporter(List(failingReporter, reporter2))

      sut
        .onRunFinished(runReport)
        .intercept[RuntimeException] >>
        reporter2.onRunFinishedCalls.assert(_.length == 1)

    }

    describe("logging") {
      val reporter1 = ReporterStub()

      test("should log and throw if a reporter throws an exception") {
        val failingReporter = ReporterStub.throwsException(new RuntimeException("Something happened"))
        val sut = new AggregateReporter(List(failingReporter, reporter1))

        sut
          .onRunFinished(runReport)
          .interceptMessage[RuntimeException]("Something happened")
      }

      test("should not log warnings if no exceptions occur") {
        val consoleReporterStub = ReporterStub()
        val sut = new AggregateReporter(List(consoleReporterStub, reporter1))

        sut.onRunFinished(runReport) >>
          consoleReporterStub.onRunFinishedCalls.assert(_.length == 1) >>
          IO(assertNotLoggedWarn("Reporter failed to report"))
      }
    }
  }
}
