package stryker4s.run

import org.apache.maven.execution.MavenSession
import org.apache.maven.plugin.AbstractMojo
import org.apache.maven.plugins.annotations.{Mojo, Parameter}

/** The main goal for this plugin. Starts Stryker4s.
  */
@Mojo(name = "run")
class Stryker4sMavenPlugin extends AbstractMojo {

  @Parameter(defaultValue = "${session}")
  private var session: MavenSession = _

  override def execute(): Unit = {
    session.getSystemProperties

    new Stryker4sMavenRunner(session.getCurrentProject).run()
  }
}
