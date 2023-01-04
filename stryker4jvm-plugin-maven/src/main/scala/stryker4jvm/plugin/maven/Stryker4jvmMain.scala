package stryker4jvm.plugin.maven

import cats.effect.unsafe.IORuntime
import org.apache.maven.plugin.{AbstractMojo, MojoFailureException}
import org.apache.maven.plugins.annotations.{Mojo, Parameter}
import org.apache.maven.project.MavenProject
import org.apache.maven.shared.invoker.DefaultInvoker
import stryker4jvm.core.logging.Logger
import stryker4jvm.plugin.maven.logging.FansiMavenMojoLogger
import stryker4jvm.run.threshold.ErrorStatus

import javax.inject.Inject

/** The main goal for this plugin. Starts Stryker4s.
  */
@Mojo(name = "run")
class Stryker4jvmMain @Inject()(@Parameter(defaultValue = "${project}") project: MavenProject) extends AbstractMojo {
  override def execute(): Unit = {
    implicit val runtime: IORuntime = IORuntime.global
    implicit val logger: Logger = new FansiMavenMojoLogger(getLog).logger
    new Stryker4jvmMavenRunner(project, new DefaultInvoker())
      .run()
      .map {
        case ErrorStatus => throw new MojoFailureException("Mutation score was below configured threshold")
        case _           =>
      }
      .unsafeRunSync()
  }
}
