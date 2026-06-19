package stryker4s.maven

import cats.effect.std.Dispatcher
import cats.effect.unsafe.IORuntime
import cats.effect.{Deferred, IO}
import org.apache.maven.execution.MavenSession
import org.apache.maven.plugin.{AbstractMojo, MojoFailureException}
import org.apache.maven.plugins.annotations.{Mojo, Parameter, ResolutionScope}
import org.apache.maven.project.MavenProject
import org.eclipse.aether.RepositorySystem
import stryker4s.log.{Logger, MavenMojoLogger}
import stryker4s.maven.runner.{ArtifactResolver, MavenCompiler, ZincCompiler}
import stryker4s.run.threshold.ErrorStatus

import javax.inject.Inject
import scala.concurrent.duration.FiniteDuration

/** The main goal for this plugin. Starts Stryker4s.
  *
  * Requires test-scope dependency resolution so the project's test classpath is available to extract.
  */
@Mojo(name = "run", requiresDependencyResolution = ResolutionScope.TEST)
class Stryker4sMain @Inject() (
    @Parameter(defaultValue = "${project}", readonly = true) project: MavenProject,
    @Parameter(defaultValue = "${session}", readonly = true) session: MavenSession,
    repoSystem: RepositorySystem
) extends AbstractMojo {
  override def execute(): Unit = {
    given IORuntime = IORuntime.global
    given Logger = new MavenMojoLogger(getLog())

    val resolver = new ArtifactResolver(project, session, repoSystem)

    val stryker = Dispatcher
      .parallel[IO]
      .evalMap: dispatcher =>
        Deferred[IO, FiniteDuration]
          .map: timeout =>
            val compiler = new MavenCompiler(project, ZincCompiler.make(project, resolver))
            new Stryker4sMavenRunner(project, resolver, compiler, timeout, dispatcher)

    stryker
      .use(_.run())
      .flatMap:
        case ErrorStatus => IO.raiseError(new MojoFailureException("Mutation score was below configured threshold"))
        case _           => IO.unit
      .unsafeRunSync()
  }
}
