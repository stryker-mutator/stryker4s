package stryker4s.maven

import cats.data.NonEmptyList
import cats.effect.{IO, Resource}
import fs2.io.file.Path
import org.apache.maven.project.MavenProject
import org.apache.maven.shared.invoker.Invoker
import stryker4s.config.Config
import stryker4s.log.Logger
import stryker4s.maven.runner.MavenTestRunner
import stryker4s.model.CompilerErrMsg
import stryker4s.mutants.applymutants.ActiveMutationContext.{envVar, ActiveMutationContext}
import stryker4s.run.Stryker4sRunner

import java.util.Properties

class Stryker4sMavenRunner(project: MavenProject, invoker: Invoker)(implicit log: Logger) extends Stryker4sRunner {

  override def mutationActivation(implicit config: Config): ActiveMutationContext = envVar

  override def resolveTestRunners(
      tmpDir: Path
  )(implicit config: Config): Either[NonEmptyList[CompilerErrMsg], NonEmptyList[Resource[IO, MavenTestRunner]]] = {
    val goals = List("test")

    val properties = new Properties(project.getProperties)
    setTestProperties(properties, config.testFilter)
    invoker.setWorkingDirectory(tmpDir.toNioPath.toFile())

    Right(NonEmptyList.of(Resource.pure[IO, MavenTestRunner](new MavenTestRunner(project, invoker, properties, goals))))
  }

  private def setTestProperties(properties: Properties, testFilter: Seq[String]): Unit = {
    // Stop after first failure. Only works with surefire plugin, not scalatest
    properties.setProperty(
      "surefire.skipAfterFailureCount",
      1.toString
    )

    // https://maven.apache.org/surefire/maven-surefire-plugin/examples/single-test.html
    val surefireFilter = "test"
    // https://www.scalatest.org/user_guide/using_the_scalatest_maven_plugin
    val scalatestFilter = "wildcardSuites"

    if (testFilter.nonEmpty) {
      if (properties.getProperty(surefireFilter) != null) {
        val newTestProperty = properties.getProperty(surefireFilter) +: testFilter
        properties.setProperty(surefireFilter, newTestProperty.mkString(", "))
      } else if (properties.getProperty(scalatestFilter) != null) {
        val newTestProperty = properties.getProperty(scalatestFilter) +: testFilter
        properties.setProperty(scalatestFilter, newTestProperty.mkString(","))
      } else {
        properties.setProperty(surefireFilter, testFilter.mkString(", "))
        properties.setProperty(scalatestFilter, testFilter.mkString(","))
      }
    }

  }
}
