package stryker4s.report

import cats.effect.{Deferred, IO}
import fs2.Stream
import stryker4s.testkit.{LogMatchers, Stryker4sIOSuite}
import stryker4s.testutil.TestData
import stryker4s.testutil.stubs.ReporterStub

class AggregateReporterTest extends Stryker4sIOSuite with LogMatchers with TestData {

  test("mutantTested should do nothing if there are no reporters") {
    val sut = new AggregateReporter(List.empty)

    Stream(MutantTestedEvent(1), MutantTestedEvent(2))
      .through(sut.mutantTested)
      .compile
      .count
      .assertEquals(0L)
  }

  test("mutantTested should report to all reporters that a mutant is tested") {
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

  test("mutantTested should report to all reporters even if a first reporter fails") {
    Deferred[IO, Unit].flatMap { failed =>
      val reporter1 = ReporterStub()
      val failingReporter = new Reporter {
        override def mutantTested: fs2.Pipe[IO, MutantTestedEvent, Nothing] =
          _.evalMap(_ => IO.raiseError[Nothing](new RuntimeException("Something happened")))
            .onFinalize(failed.complete(()).void)
      }
      val sut = new AggregateReporter(List(failingReporter, reporter1))
      val events = (1 to 100).map(MutantTestedEvent(_)).toList

      (Stream.emit(events.head) ++ Stream.exec(failed.get) ++ Stream.emits(events.tail))
        .through(sut.mutantTested)
        .compile
        .drain
        .assertLoggedError(
          "Reporter failed to report, java.lang.RuntimeException: Something happened"
        ) >> reporter1.mutantTestedCalls.assertEquals(events)
    }
  }

  test("mutantTested should still log input stream failures") {
    val sut = new AggregateReporter(List(ReporterStub()))

    Stream
      .raiseError[IO](new RuntimeException("Input failed"))
      .through(sut.mutantTested)
      .compile
      .drain
      .assertLoggedError("Reporter failed to report, java.lang.RuntimeException: Input failed")
  }

  val runReport = createFinishedRunEvent()

  test("onRunFinished should report to all reporters that a mutation run is completed") {
    val reporter1 = ReporterStub()
    val reporter2 = ReporterStub()
    val sut: AggregateReporter = new AggregateReporter(List(reporter1, reporter2))

    sut
      .onRunFinished(runReport) >>
      reporter1.onRunFinishedCalls.assert(_.length == 1) *>
      reporter2.onRunFinishedCalls.assert(_.length == 1)

  }

  test("onRunFinished should still call other reporters if a reporter throws an exception") {
    val failingReporter = ReporterStub.throwsException(new RuntimeException("Something happened"))
    val reporter2 = ReporterStub()
    val sut = new AggregateReporter(List(failingReporter, reporter2))

    sut
      .onRunFinished(runReport)
      .intercept[RuntimeException] >>
      reporter2.onRunFinishedCalls.assert(_.length == 1)

  }

  val reporter1 = ReporterStub()

  test("onRunFinished logging should log and throw if a reporter throws an exception") {
    val failingReporter = ReporterStub.throwsException(new RuntimeException("Something happened"))
    val sut = new AggregateReporter(List(failingReporter, reporter1))

    sut
      .onRunFinished(runReport)
      .interceptMessage[RuntimeException]("Something happened")
  }

  test("onRunFinished logging should not log warnings if no exceptions occur") {
    val consoleReporterStub = ReporterStub()
    val sut = new AggregateReporter(List(consoleReporterStub, reporter1))

    sut.onRunFinished(runReport) >>
      consoleReporterStub.onRunFinishedCalls
        .assert(_.length == 1)
        .assertNotLoggedWarn("Reporter failed to report")
  }
}
