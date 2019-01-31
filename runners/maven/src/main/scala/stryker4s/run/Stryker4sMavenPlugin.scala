package stryker4s.run

import org.apache.maven.model.Build
import org.apache.maven.plugin.AbstractMojo
import org.apache.maven.plugins.annotations.{Mojo, Parameter}

/** The main goal for this plugin. Starts Stryker4s.
  */
@Mojo(name = "run")
class Stryker4sMavenPlugin(@Parameter(defaultValue = "${build}") build: Build) extends AbstractMojo {

  override def execute(): Unit = {
    new Stryker4sMavenRunner(build).run()
  }
}
