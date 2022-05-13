package stryker4s.maven

import cats.effect.unsafe.IORuntime
import org.apache.maven.plugin.{AbstractMojo, MojoFailureException}
import org.apache.maven.plugins.annotations.{Mojo, Parameter}
import org.apache.maven.project.MavenProject
import org.apache.maven.shared.invoker.DefaultInvoker
import stryker4s.log.{Logger, MavenMojoLogger}
import stryker4s.run.threshold.ErrorStatus

import javax.inject.Inject

/** The main goal for this plugin. Starts Stryker4s.
  */
@Mojo(name = "run")
class Stryker4sMain @Inject(@Parameter(defaultValue = "${project}") project: MavenProject) extends AbstractMojo {
  override def execute(): Unit = {
    implicit val runtime = IORuntime.global
    implicit val logger: Logger = new MavenMojoLogger(getLog())
    new Stryker4sMavenRunner(project, new DefaultInvoker)
      .run()
      .map {
        case ErrorStatus => throw new MojoFailureException("Mutation score was below configured threshold")
        case _           =>
      }
      .unsafeRunSync()
  }
}
