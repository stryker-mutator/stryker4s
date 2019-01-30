package stryker4s.run
import java.nio.file.Path

import better.files._
import org.apache.maven.project.MavenProject
import org.apache.maven.shared.invoker.{DefaultInvocationRequest, InvocationRequest, Invoker}
import stryker4s.config.Config
import stryker4s.extension.FileExtensions._
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

  override def runInitialTest(workingDir: File): Boolean = {
    val build = project.getBuild
    val sources = tmpDir / build.getSourceDirectory.toFile.relativePath.toString
    // Set source once, settings is persistent among goals
    build.setSourceDirectory(sources.pathAsString) // TODO: Doesn't work yet
    project.setBuild(build)

    val request = createRequest(workingDir)
    request.setGoals(goals)

    val result = invoker.execute(request)

    result.getExitCode == 0
  }

  override def runMutant(mutant: Mutant, workingDir: File, subPath: Path): MutantRunResult = {
    val request = createRequest(workingDir)
    request.addShellEnvironment("ACTIVE_MUTATION", String.valueOf(mutant.id))

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
