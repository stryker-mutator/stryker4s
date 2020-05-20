package stryker4s.maven.runner

import java.nio.file.Path
import java.util.Properties

import better.files._
import org.apache.maven.project.MavenProject
import org.apache.maven.shared.invoker.{DefaultInvocationRequest, InvocationRequest, Invoker}
import stryker4s.config.Config
import stryker4s.model.{Killed, Mutant, MutantRunResult, Survived}
import stryker4s.mutants.findmutants.SourceCollector
import stryker4s.report.Reporter
import stryker4s.run.MutantRunner

import scala.collection.JavaConverters._
import stryker4s.config.TestFilter

class MavenMutantRunner(project: MavenProject, invoker: Invoker, sourceCollector: SourceCollector, reporter: Reporter)(
    implicit config: Config
) extends MutantRunner(sourceCollector, reporter) {
  private val goals = List("test").asJava

  private val properties = new Properties(project.getProperties)
  setTestProperties()
  invoker.setWorkingDirectory(tmpDir.toJava)

  override def runInitialTest(workingDir: File): Boolean = {
    val request = createRequest()

    val result = invoker.execute(request)

    result.getExitCode == 0
  }

  override def runMutant(mutant: Mutant, workingDir: File): Path => MutantRunResult = {
    val request = createRequestWithMutation(mutant)

    val result = invoker.execute(request)

    result.getExitCode match {
      case 0 => Survived(mutant, _)
      case _ => Killed(mutant, _)
    }
  }

  private def createRequest(): InvocationRequest =
    new DefaultInvocationRequest()
      .setGoals(goals)
      .setOutputHandler(debug(_))
      .setBatchMode(true)
      .setProperties(properties)

  private def createRequestWithMutation(mutant: Mutant): InvocationRequest =
    createRequest()
      .addShellEnvironment("ACTIVE_MUTATION", String.valueOf(mutant.id))

  private def setTestProperties(): Unit = {
    // Stop after first failure. Only works with surefire plugin, not scalatest
    properties.setProperty(
      "surefire.skipAfterFailureCount",
      1.toString
    )

    // https://maven.apache.org/surefire/maven-surefire-plugin/examples/single-test.html
    val surefireFilter = "test"
    // https://www.scalatest.org/user_guide/using_the_scalatest_maven_plugin
    val scalatestFilter = "wildcardSuites"

    if (config.testFilter.nonEmpty) {
      if (properties.getProperty(surefireFilter) != null) {
        val newTestProperty = properties.getProperty(surefireFilter) +: config.testFilter
        properties.setProperty(surefireFilter, newTestProperty.mkString(", "))

      } else if (properties.getProperty(scalatestFilter) != null) {
        val newTestProperty = properties.getProperty(scalatestFilter) +: config.testFilter
        properties.setProperty(scalatestFilter, newTestProperty.mkString(","))

      } else {
        properties.setProperty(surefireFilter, config.testFilter.mkString(", "))
        properties.setProperty(scalatestFilter, config.testFilter.mkString(","))
      }
    }

  }
}
