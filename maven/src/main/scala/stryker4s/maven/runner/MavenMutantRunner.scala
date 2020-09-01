package stryker4s.maven.runner

import java.nio.file.Path
import java.util.Properties

import better.files._
import org.apache.maven.project.MavenProject
import org.apache.maven.shared.invoker.{DefaultInvocationRequest, InvocationRequest, Invoker}
import stryker4s.config.Config
import stryker4s.model.{Killed, MavenRunnerContext, Mutant, MutantRunResult, Survived}
import stryker4s.mutants.findmutants.SourceCollector
import stryker4s.report.Reporter
import stryker4s.run.MutantRunner

import scala.collection.JavaConverters._
import stryker4s.config.TestFilter
import cats.effect.{ContextShift, IO, Resource}

class MavenMutantRunner(project: MavenProject, invoker: Invoker, sourceCollector: SourceCollector, reporter: Reporter)(
    implicit
    config: Config,
    cs: ContextShift[IO]
) extends MutantRunner(sourceCollector, reporter) {
  type Context = MavenRunnerContext

  def initializeTestContext(tmpDir: File): Resource[IO, Context] = {
    val goals = List("test")

    val properties = new Properties(project.getProperties)
    setTestProperties(properties)
    invoker.setWorkingDirectory(tmpDir.toJava)

    Resource.pure[IO, Context](MavenRunnerContext(properties, goals, tmpDir))
  }

  override def runInitialTest(context: Context): IO[Boolean] = {
    val request = createRequest(context)

    IO(invoker.execute(request)).map(_.getExitCode() == 0)
  }

  override def runMutant(mutant: Mutant, context: Context): IO[MutantRunResult] = {
    val request = createRequestWithMutation(mutant, context)

    IO(invoker.execute(request)).map { result =>
      result.getExitCode match {
        case 0 => Survived(mutant)
        case _ => Killed(mutant)
      }
    }
  }

  private def createRequest(context: Context): InvocationRequest =
    new DefaultInvocationRequest()
      .setGoals(context.goals.asJava)
      .setOutputHandler(debug(_))
      .setBatchMode(true)
      .setProperties(context.properties)
      .setProfiles(project.getActiveProfiles.asScala.map(_.getId).asJava)

  private def createRequestWithMutation(mutant: Mutant, context: Context): InvocationRequest =
    createRequest(context)
      .addShellEnvironment("ACTIVE_MUTATION", String.valueOf(mutant.id))

  private def setTestProperties(properties: Properties): Unit = {
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
