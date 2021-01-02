package stryker4s.maven.runner

import java.util.Properties

import scala.collection.JavaConverters._

import better.files._
import cats.effect.{ContextShift, IO, Resource, Timer}
import org.apache.maven.project.MavenProject
import org.apache.maven.shared.invoker.{DefaultInvocationRequest, InvocationRequest, Invoker}
import stryker4s.config.Config
import stryker4s.log.Logger
import stryker4s.model.MutantRunResult
import stryker4s.mutants.findmutants.SourceCollector
import stryker4s.report.Reporter
import stryker4s.run.{InitialTestRunResult, MutantRunner, TestRunner}

class MavenMutantRunner(project: MavenProject, invoker: Invoker, sourceCollector: SourceCollector, reporter: Reporter)(
    implicit
    config: Config,
    log: Logger,
    timer: Timer[IO],
    cs: ContextShift[IO]
) extends MutantRunner(sourceCollector, reporter) {

  def initializeTestRunner(tmpDir: File): Resource[IO, MavenTestRunner] = {
    val goals = List("test")

    val properties = new Properties(project.getProperties)
    setTestProperties(properties)
    invoker.setWorkingDirectory(tmpDir.toJava)

    Resource.pure[IO, MavenTestRunner](new MavenTestRunner(project, invoker, properties, goals))
  }

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
        ()
      } else if (properties.getProperty(scalatestFilter) != null) {
        val newTestProperty = properties.getProperty(scalatestFilter) +: config.testFilter
        properties.setProperty(scalatestFilter, newTestProperty.mkString(","))
        ()
      } else {
        properties.setProperty(surefireFilter, config.testFilter.mkString(", "))
        properties.setProperty(scalatestFilter, config.testFilter.mkString(","))
        ()
      }
    }

  }
}
