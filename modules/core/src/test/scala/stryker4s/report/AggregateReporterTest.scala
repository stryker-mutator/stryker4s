package stryker4s.report

import cats.effect.{IO, Ref}
import cats.syntax.all.*
import fs2.{Pipe, Stream}
import stryker4s.testkit.{LogMatchers, MockitoSuite, Stryker4sIOSuite}
import stryker4s.testutil.TestData

class AggregateReporterTest extends Stryker4sIOSuite with MockitoSuite with LogMatchers with TestData {

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
      (createMutantTestedReporter, createMutantTestedReporter).tupled.flatMap {
        case ((completed1, reporter1), (completed2, reporter2)) =>
          val sut = new AggregateReporter(List(reporter1, reporter2))

          Stream(MutantTestedEvent(1), MutantTestedEvent(2))
            .through(sut.mutantTested)
            .compile
            .drain >> {
            completed1.get.assertEquals(true) *>
              completed2.get.assertEquals(true)
          }
      }
    }

    test("should report to all reporters even if a first reporter fails") {
      createMutantTestedReporter.flatMap { case (completed1, reporter1) =>
        val failingReporter = new Reporter {
          override def mutantTested: Pipe[IO, MutantTestedEvent, Nothing] =
            _ *> Stream.eval(IO.cede) *> (Stream.raiseError[IO](new RuntimeException("Something happened")))
        }
        val sut = new AggregateReporter(List(failingReporter, reporter1))

        Stream(MutantTestedEvent(1), MutantTestedEvent(2))
          .through(sut.mutantTested)
          .compile
          .drain >> {
          assertLoggedError("Reporter failed to report, java.lang.RuntimeException: Something happened")
          completed1.get.assertEquals(true)
        }
      }
    }

    def createMutantTestedReporter: IO[(Ref[IO, Boolean], Reporter)] = Ref[IO].of(false).map { completed =>
      (
        completed,
        new Reporter {
          override def mutantTested: Pipe[IO, MutantTestedEvent, Nothing] =
            in => in.evalMap(_ => completed.set(true)).drain
        }
      )
    }
  }

  describe("onRunFinished") {
    val runReport = createFinishedRunEvent()

    test("should report to all reporters that a mutation run is completed") {
      val reporter1 = mock[Reporter]
      val reporter2 = mock[Reporter]
      whenF(reporter1.onRunFinished(any[FinishedRunEvent])).thenReturn(())
      whenF(reporter2.onRunFinished(any[FinishedRunEvent])).thenReturn(())
      val sut: AggregateReporter = new AggregateReporter(List(reporter1, reporter2))

      sut
        .onRunFinished(runReport)
        .asserting { _ =>
          verify(reporter1).onRunFinished(runReport)
          verify(reporter2).onRunFinished(runReport)
          ()
        }

    }

    test("should still call other reporters if a reporter throws an exception") {
      val failingReporter = mock[Reporter]
      val reporter2 = mock[Reporter]
      whenF(failingReporter.onRunFinished(any[FinishedRunEvent]))
        .thenFailWith(new RuntimeException("Something happened"))
      whenF(reporter2.onRunFinished(any[FinishedRunEvent])).thenReturn(())
      val sut = new AggregateReporter(List(failingReporter, reporter2))

      sut
        .onRunFinished(runReport)
        .asserting { _ =>
          verify(reporter2).onRunFinished(runReport)
          ()
        }
        .intercept[RuntimeException]
    }

    describe("logging") {
      val reporter1 = mock[Reporter]
      whenF(reporter1.onRunFinished(any[FinishedRunEvent])).thenReturn(())

      test("should log and throw if a reporter throws an exception") {
        val failingReporter = mock[ConsoleReporter]
        val sut = new AggregateReporter(List(failingReporter, reporter1))
        whenF(failingReporter.onRunFinished(runReport))
          .thenFailWith(new RuntimeException("Something happened"))

        sut
          .onRunFinished(runReport)
          .interceptMessage[RuntimeException]("Something happened")
      }

      test("should not log warnings if no exceptions occur") {
        val consoleReporterMock = mock[ConsoleReporter]
        whenF(consoleReporterMock.onRunFinished(any[FinishedRunEvent])).thenReturn(())
        val sut = new AggregateReporter(List(consoleReporterMock, reporter1))

        sut
          .onRunFinished(runReport)
          .asserting { _ =>
            verify(consoleReporterMock).onRunFinished(runReport)
            assertNotLoggedWarn("Reporter failed to report")
          }
      }
    }
  }
}
