package stryker4s.run

import org.apache.maven.plugin.AbstractMojo
import org.apache.maven.plugins.annotations.Mojo

@Mojo(name = "run")
class Stryker4sMavenPlugin extends AbstractMojo {
  override def execute(): Unit = {
    new Stryker4sMavenRunner().run()
  }
}
