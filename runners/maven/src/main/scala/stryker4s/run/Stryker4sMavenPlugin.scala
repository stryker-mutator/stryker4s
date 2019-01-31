package stryker4s.run

import javax.inject.Inject
import org.apache.maven.plugin.AbstractMojo
import org.apache.maven.plugins.annotations.{Mojo, Parameter}
import org.apache.maven.project.MavenProject

/** The main goal for this plugin. Starts Stryker4s.
  */
@Mojo(name = "run")
class Stryker4sMavenPlugin @Inject()(@Parameter(defaultValue = "${project}") project: MavenProject)
    extends AbstractMojo {
  override def execute(): Unit = {
    new Stryker4sMavenRunner(project).run()
  }
}
