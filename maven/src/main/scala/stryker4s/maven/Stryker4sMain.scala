package stryker4s.maven

import scala.concurrent.ExecutionContext
import scala.concurrent.ExecutionContext.Implicits.global

import cats.effect.{ContextShift, IO, Timer}
import javax.inject.Inject
import org.apache.maven.plugin.{AbstractMojo, MojoFailureException}
import org.apache.maven.plugins.annotations.{Mojo, Parameter}
import org.apache.maven.project.MavenProject
import stryker4s.run.threshold.ErrorStatus
import stryker4s.log.{Logger, MavenMojoLogger}

/** The main goal for this plugin. Starts Stryker4s.
  */
@Mojo(name = "run")
class Stryker4sMain @Inject() (@Parameter(defaultValue = "${project}") project: MavenProject) extends AbstractMojo {
  override def execute(): Unit = {
    implicit val cs: ContextShift[IO] = IO.contextShift(implicitly[ExecutionContext])
    implicit val timer: Timer[IO] = IO.timer(implicitly[ExecutionContext])
    implicit val logger: Logger = new MavenMojoLogger(getLog())
    new Stryker4sMavenRunner(project)
      .run()
      .map {
        case ErrorStatus => throw new MojoFailureException("Mutation score was below configured threshold")
        case _           =>
      }
      .unsafeRunSync()
  }
}
