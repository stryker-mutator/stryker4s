package stryker4s.run
import java.nio.file.Path
import java.util.Properties

import better.files._
import org.apache.maven.model.Build
import org.apache.maven.shared.invoker.{DefaultInvocationRequest, InvocationRequest, Invoker}
import stryker4s.config.Config
import stryker4s.model.{Killed, Mutant, MutantRunResult, Survived}
import stryker4s.mutants.findmutants.SourceCollector
import stryker4s.run.process.ProcessRunner

import scala.collection.JavaConverters._

class MavenMutantRunner(build: Build, invoker: Invoker, processRunner: ProcessRunner, sourceCollector: SourceCollector)(
    implicit config: Config
) extends MutantRunner(processRunner, sourceCollector) {

  private val goals = List("test").asJava
  private val properties = new Properties()

  override def runInitialTest(workingDir: File): Boolean = {
    // Set source once, settings is persistent among goals
    build.setDirectory(workingDir.pathAsString) // TODO: Doesn't work yet

    val request = createRequest(workingDir)
    request.setGoals(goals)

    val result = invoker.execute(request)

    result.getExitCode == 0
  }

  override def runMutant(mutant: Mutant, workingDir: File, subPath: Path): MutantRunResult = {
    val request = createRequest(workingDir)
    properties.setProperty("ACTIVE_MUTATION", String.valueOf(mutant.id))
    request.setProperties(properties)

    val result = invoker.execute(request)

    result.getExitCode match {
      case 0 => Survived(mutant, subPath)
      case _ => Killed(mutant, subPath)
    }
  }

  private def createRequest(tmpDir: File): InvocationRequest = {
    new DefaultInvocationRequest()
      .setGoals(goals)
      .setOutputHandler(debug(_))
      .setBatchMode(true)
  }
}
