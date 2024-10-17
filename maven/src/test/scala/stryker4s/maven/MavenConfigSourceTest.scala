package stryker4s.maven

import cats.effect.IO
import org.apache.maven.project.MavenProject
import stryker4s.testkit.Stryker4sIOSuite
import fs2.io.file.Path

class MavenConfigSourceTest extends Stryker4sIOSuite {
  test("should load a filled config") {
    val project = new MavenProject()
    project.addCompileSourceRoot("src/main/scala")
    val config = new MavenConfigSource[IO](project)

    config.mutate.load.assertEquals(Seq(Path("src/main/scala/**.scala").absolute.toString))
  }

  test("fails on unsupported values") {
    new MavenConfigSource[IO](new MavenProject()).testFilter.attempt
      .map(_.leftValue.messages.loneElement)
      .assertEquals("Missing key testFilter is not supported by maven")
  }
}
