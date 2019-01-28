package stryker4s.run
import java.nio.file.Path

import better.files.File
import org.apache.maven.shared.invoker.{DefaultInvocationRequest, InvocationRequest, Invoker}
import stryker4s.config.Config
import stryker4s.model.{Killed, Mutant, MutantRunResult, Survived}
import stryker4s.mutants.findmutants.SourceCollector
import stryker4s.run.process.ProcessRunner

import scala.collection.JavaConverters._

class MavenMutantRunner(invoker: Invoker, processRunner: ProcessRunner, sourceCollector: SourceCollector)(
    implicit config: Config)
    extends MutantRunner(processRunner, sourceCollector) {

  private val goal = List("test").asJava

  override def runInitialTest(workingDir: File): Boolean = {
    val request = createRequest(workingDir)

    val result = invoker.execute(request)

    result.getExitCode == 0
  }

  override def runMutant(mutant: Mutant, workingDir: File, subPath: Path): MutantRunResult = {
    val request = createRequest(workingDir)

    val result = invoker.execute(request)

    result.getExitCode match {
      case 0 => Survived(mutant, subPath)
      case _ => Killed(mutant, subPath)
    }
  }

  private def createRequest(sourceDir: File): InvocationRequest = {
    val request = new DefaultInvocationRequest

    // TODO: Set working dir
    request.setGoals(goal)
    request.setOutputHandler(debug(_))
    request.setBatchMode(true)

    request
  }
}
