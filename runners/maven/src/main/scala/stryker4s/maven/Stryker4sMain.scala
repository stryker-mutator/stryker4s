package stryker4s.maven

import javax.inject.Inject
import org.apache.maven.plugin.{AbstractMojo, MojoFailureException}
import org.apache.maven.plugins.annotations.{Mojo, Parameter}
import org.apache.maven.project.MavenProject
import stryker4s.run.threshold.ErrorStatus

/** The main goal for this plugin. Starts Stryker4s.
  */
@Mojo(name = "run")
class Stryker4sMain @Inject()(@Parameter(defaultValue = "${project}") project: MavenProject)
    extends AbstractMojo {
  override def execute(): Unit = {
    new Stryker4sMavenRunner(project).run() match {
      case ErrorStatus => throw new MojoFailureException("Mutation score was below configured threshold")
      case _           =>
    }
  }
}
