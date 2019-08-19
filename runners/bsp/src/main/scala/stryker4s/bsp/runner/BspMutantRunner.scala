package stryker4s.bsp.runner

import java.nio.file.Path

import better.files.File
import ch.epfl.scala.bsp.StatusCode
import stryker4s.bsp.BspContext
import stryker4s.bsp.connection.TestRequest
import stryker4s.config.Config
import stryker4s.model.{Killed, Mutant, MutantRunResult, Survived}
import stryker4s.mutants.findmutants.SourceCollector
import stryker4s.report.Reporter
import stryker4s.run.MutantRunner

class BspMutantRunner(bspContext: BspContext, sourceCollector: SourceCollector, reporter: Reporter)(
    implicit config: Config
) extends MutantRunner(sourceCollector, reporter) {

  override def runMutant(mutant: Mutant, workingDir: File): Path => MutantRunResult = {
    import stryker4s.bsp.connection.BspRequestSender.testRequestSender
    implicit val req = testRequestSender("")
    bspContext.send[TestRequest](mutant.id.toString)
    StatusCode.Ok match {
      case StatusCode.Ok => Survived(mutant, _)
      case _             => Killed(mutant, _)
    }
  }
  override def runInitialTest(workingDir: File): Boolean = bspContext.send[TestRequest]("None") == StatusCode.Ok
}
