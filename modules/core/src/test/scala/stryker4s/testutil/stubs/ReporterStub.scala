package stryker4s.testutil.stubs

import cats.effect.{IO, Ref}
import fs2.Pipe
import stryker4s.report.{FinishedRunEvent, MutantTestedEvent, Reporter}

trait ReporterStub extends Reporter {
  def mutantTestedCalls: IO[Seq[MutantTestedEvent]]
  def onRunFinishedCalls: IO[Seq[FinishedRunEvent]]
}

object ReporterStub {
  def apply(): ReporterStub = {
    val mutantTestedCallsRef = Ref.unsafe[IO, Seq[MutantTestedEvent]](Seq.empty)
    val onRunFinishedCallsRef = Ref.unsafe[IO, Seq[FinishedRunEvent]](Seq.empty)
    new ReporterStub {

      def mutantTestedCalls: IO[Seq[MutantTestedEvent]] = mutantTestedCallsRef.get
      def onRunFinishedCalls: IO[Seq[FinishedRunEvent]] = onRunFinishedCallsRef.get

      override def mutantTested: Pipe[IO, MutantTestedEvent, Nothing] =
        _.evalMap(in => mutantTestedCallsRef.update(_ :+ in)).drain
      override def onRunFinished(runReport: FinishedRunEvent): IO[Unit] = onRunFinishedCallsRef.update(_ :+ runReport)
    }
  }

  def throwsException(e: Exception): Reporter = new Reporter {
    override def mutantTested: Pipe[IO, MutantTestedEvent, Nothing] = _.evalMap(_ => IO.raiseError(e)).drain
    override def onRunFinished(runReport: FinishedRunEvent): IO[Unit] = IO.raiseError(e)
  }
}
