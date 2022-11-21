package stryker4jvm.reporting

import cats.effect.{IO, Ref}
import cats.syntax.all.*
import fs2.{Pipe, Stream}
import org.scalatest.TestData
import stryker4jvm.scalatest.LogMatchers
import stryker4jvm.testutil.{MockitoIOSuite, Stryker4jvmIOSuite}

class AggregateReporterTest extends Stryker4jvmIOSuite with MockitoIOSuite with LogMatchers with TestData {

  describe("mutantTested") {
    it("should do nothing if there are no reporters") {
      val sut = new AggregateReporter(List.empty)

      Stream(MutantTestedEvent(1), MutantTestedEvent(2))
        .through(sut.mutantTested)
        .compile
        .count
        .asserting(_ shouldBe 0)
    }

    it("should report to all reporters that a mutant is tested") {
      (createMutantTestedReporter, createMutantTestedReporter).tupled.flatMap {
        case ((completed1, reporter1), (completed2, reporter2)) =>
          val sut = new AggregateReporter(List(reporter1, reporter2))

          Stream(MutantTestedEvent(1), MutantTestedEvent(2))
            .through(sut.mutantTested)
            .compile
            .drain >> {
            completed1.get.asserting(_ shouldBe true) *>
              completed2.get.asserting(_ shouldBe true)
          }
      }
    }

    it("should report to all reporters even if a first reporter fails") {
      createMutantTestedReporter.flatMap { case (completed1, reporter1) =>
        val failingReporter = new Reporter {
          override def mutantTested: Pipe[IO, MutantTestedEvent, Nothing] =
            _ *> (Stream.raiseError[IO](new RuntimeException("Something happened")))
        }
        val sut = new AggregateReporter(List(failingReporter, reporter1))

        Stream(MutantTestedEvent(1), MutantTestedEvent(2))
          .through(sut.mutantTested)
          .compile
          .drain >> {
          "Reporter failed to report, java.lang.RuntimeException: Something happened" shouldBe loggedAsError
          completed1.get.asserting(_ shouldBe true)
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

    it("should report to all reporters that a mutation run is completed") {
      val reporter1 = mock[Reporter]
      val reporter2 = mock[Reporter]
      whenF(reporter1.onRunFinished(any[FinishedRunEvent])).thenReturn(())
      whenF(reporter2.onRunFinished(any[FinishedRunEvent])).thenReturn(())
      val sut: AggregateReporter = new AggregateReporter(List(reporter1, reporter2))

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
      whenF(failingReporter.onRunFinished(any[FinishedRunEvent]))
        .thenFailWith(new RuntimeException("Something happened"))
      whenF(reporter2.onRunFinished(any[FinishedRunEvent])).thenReturn(())
      val sut = new AggregateReporter(List(failingReporter, reporter2))

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
        val sut = new AggregateReporter(List(failingReporter, reporter1))
        whenF(failingReporter.onRunFinished(runReport))
          .thenFailWith(new RuntimeException("Something happened"))

        sut
          .onRunFinished(runReport)
          .attempt
          .asserting {
            case Left(e: RuntimeException) =>
              e.getMessage() shouldBe "Something happened"
            case r => fail(s"Expected RuntimeException, got $r")
          }
      }

      it("should not log warnings if no exceptions occur") {
        val consoleReporterMock = mock[ConsoleReporter]
        whenF(consoleReporterMock.onRunFinished(any[FinishedRunEvent])).thenReturn(())
        val sut = new AggregateReporter(List(consoleReporterMock, reporter1))

        sut
          .onRunFinished(runReport)
          .asserting { _ =>
            verify(consoleReporterMock).onRunFinished(runReport)
            "Reporter failed to report" should not be loggedAsWarning
          }
      }
    }
  }
}
