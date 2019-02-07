package stryker4s.run
import java.nio.file.Path
import java.util.Properties

import better.files._
import org.apache.maven.project.MavenProject
import org.apache.maven.shared.invoker.{DefaultInvocationRequest, InvocationRequest, Invoker}
import stryker4s.config.Config
import stryker4s.model.{Killed, Mutant, MutantRunResult, Survived}
import stryker4s.mutants.findmutants.SourceCollector
import stryker4s.run.process.ProcessRunner

import scala.collection.JavaConverters._

class MavenMutantRunner(project: MavenProject,
                        invoker: Invoker,
                        processRunner: ProcessRunner,
                        sourceCollector: SourceCollector)(implicit config: Config)
    extends MutantRunner(processRunner, sourceCollector) {

  private val goals = List("test").asJava

  private val properties = new Properties(project.getProperties)
  properties.setProperty("surefire.skipAfterFailureCount", 1.toString) // Stop after first failure. Only works with surefire plugin, not scalatest

  override def runInitialTest(workingDir: File): Boolean = {
    // Set source once, settings is persistent among goals
    invoker.setWorkingDirectory(workingDir.toJava)

    val request = createRequest

    val result = invoker.execute(request)

    result.getExitCode == 0
  }

  override def runMutant(mutant: Mutant, workingDir: File, subPath: Path): MutantRunResult = {
    val request = createRequestWithMutation(mutant)
    request.addShellEnvironment("ACTIVE_MUTATION", String.valueOf(mutant.id))

    val result = invoker.execute(request)

    result.getExitCode match {
      case 0 => Survived(mutant, subPath)
      case _ => Killed(mutant, subPath)
    }
  }

  private def createRequest: InvocationRequest =
    new DefaultInvocationRequest()
      .setGoals(goals)
      .setOutputHandler(debug(_))
      .setBatchMode(true)
      .setProperties(properties)

  private def createRequestWithMutation(mutant: Mutant): InvocationRequest =
    createRequest
      .addShellEnvironment("ACTIVE_MUTATION", String.valueOf(mutant.id))

}
