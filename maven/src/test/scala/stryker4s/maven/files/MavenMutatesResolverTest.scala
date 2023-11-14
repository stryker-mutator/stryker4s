package stryker4s.maven.files

import org.apache.maven.project.MavenProject
import stryker4s.testkit.Stryker4sIOSuite

class MavenMutatesResolverTest extends Stryker4sIOSuite {
  test("resolves from compileSourceRoots as base") {
    val project = new MavenProject()
    project.addCompileSourceRoot("src/main/scala")
    val sut = new MavenMutatesResolver(project)

    sut.files.compile.toVector.asserting { files =>
      assert(files.nonEmpty)
      files.foreach(file => assertEquals(file.extName, ".scala"))
      assert(files.exists(_.endsWith("MavenMutatesResolver.scala")))
    }
  }

  test("resolves no files from non-existing directory") {
    val project = new MavenProject()
    project.addCompileSourceRoot("does/not/exist/src/main/scala")
    val sut = new MavenMutatesResolver(project)

    sut.files.compile.toVector.assert(_.isEmpty)
  }
}
