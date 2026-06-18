package stryker4s.maven

import cats.effect.IO
import fs2.io.file.Path
import org.apache.maven.model.{Build, Model, Plugin, Scm}
import org.apache.maven.project.MavenProject
import org.codehaus.plexus.util.xml.Xpp3Dom
import stryker4s.testkit.Stryker4sIOSuite

import scala.jdk.CollectionConverters.*
import scala.meta.dialects

class MavenConfigSourceTest extends Stryker4sIOSuite {
  test("should load a filled config") {
    val project = new MavenProject()
    project.addCompileSourceRoot("src/main/scala")
    val config = new MavenConfigSource[IO](project)

    config.mutate.load.assertEquals(Seq(Path("src/main/scala/**.scala").absolute.toString))
  }

  test("derives the scala dialect from the scala.version property") {
    val project = projectWithScalacArgs("3.3.4")

    new MavenConfigSource[IO](project).scalaDialect.load.assertEquals(dialects.Scala33)
  }

  test("derives a Scala 2.13 source3 dialect when -Xsource:3 is one of the scala-maven-plugin args") {
    val project = projectWithScalacArgs("2.13.18", "-deprecation", "-Xsource:3")

    new MavenConfigSource[IO](project).scalaDialect.load.assertEquals(dialects.Scala213Source3)
  }

  test("derives a Scala 3 dialect even when -Xsource:3 is set (it does not apply to Scala 3)") {
    val project = projectWithScalacArgs("3.3.4", "-Xsource:3")

    new MavenConfigSource[IO](project).scalaDialect.load.assertEquals(dialects.Scala33)
  }

  test("a non -Xsource:3 scalac option does not enable the source3 dialect") {
    val project = projectWithScalacArgs("2.13.18", "-deprecation")

    new MavenConfigSource[IO](project).scalaDialect.load.assertEquals(dialects.Scala213)
  }

  test("an unrecognized Scala 2 minor version falls back to Scala213Source3 with -Xsource:3") {
    val project = projectWithScalacArgs("2.20.0", "-Xsource:3")

    new MavenConfigSource[IO](project).scalaDialect.load.assertEquals(dialects.Scala213Source3)
  }

  test("an unrecognized Scala 2 minor version falls back to Scala213 without -Xsource:3") {
    val project = projectWithScalacArgs("2.20.0", "-deprecation")

    new MavenConfigSource[IO](project).scalaDialect.load.assertEquals(dialects.Scala213)
  }

  test("a version with no minor part falls back to Scala213Source3 with -Xsource:3") {
    val project = projectWithScalacArgs("2", "-Xsource:3")

    new MavenConfigSource[IO](project).scalaDialect.load.assertEquals(dialects.Scala213Source3)
  }

  test("a version with no minor part falls back to Scala213 without -Xsource:3") {
    val project = projectWithScalacArgs("2", "-deprecation")

    new MavenConfigSource[IO](project).scalaDialect.load.assertEquals(dialects.Scala213)
  }

  test("derives the source roots with a /** glob as the files config") {
    val project = new MavenProject()
    project.addCompileSourceRoot("src/main/scala")
    project.addTestCompileSourceRoot("src/test/scala")

    val roots = (project.getCompileSourceRoots().asScala ++ project.getTestCompileSourceRoots().asScala).toSeq
    assert(roots.nonEmpty)
    new MavenConfigSource[IO](project).files.load.assertEquals(roots.map(_ + "/**"))
  }

  test("derives the dashboard project from the scm connection, stripping the scm prefix and .git suffix") {
    val model = new Model()
    val scm = new Scm()
    scm.setConnection("scm:git:https://github.com/stryker-mutator/stryker4s.git")
    model.setScm(scm)

    new MavenConfigSource[IO](new MavenProject(model)).dashboardProject.load
      .assertEquals(Some("github.com/stryker-mutator/stryker4s"))
  }

  test("strips a trailing slash from the scm url path") {
    val model = new Model()
    val scm = new Scm()
    scm.setUrl("https://github.com/stryker-mutator/stryker4s/")
    model.setScm(scm)

    new MavenConfigSource[IO](new MavenProject(model)).dashboardProject.load
      .assertEquals(Some("github.com/stryker-mutator/stryker4s"))
  }

  test("does not derive a dashboard project from a non-github scm url") {
    val model = new Model()
    val scm = new Scm()
    scm.setUrl("https://gitlab.com/stryker-mutator/stryker4s")
    model.setScm(scm)

    new MavenConfigSource[IO](new MavenProject(model)).dashboardProject.attempt
      .map(_.leftValue.messages.loneElement)
      .assertEquals("Missing key dashboardProject is not supported by maven")
  }

  test("derives the dashboard module from the artifactId") {
    val model = new Model()
    model.setArtifactId("my-module")

    new MavenConfigSource[IO](new MavenProject(model)).dashboardModule.load.assertEquals(Some("my-module"))
  }

  test("derives the dashboard project from the github SCM url") {
    val model = new Model()
    val scm = new Scm()
    scm.setUrl("https://github.com/stryker-mutator/stryker4s")
    model.setScm(scm)

    new MavenConfigSource[IO](new MavenProject(model)).dashboardProject.load
      .assertEquals(Some("github.com/stryker-mutator/stryker4s"))
  }

  test("fails on unsupported values") {
    new MavenConfigSource[IO](new MavenProject()).testFilter.attempt
      .map(_.leftValue.messages.loneElement)
      .assertEquals("Missing key testFilter is not supported by maven")
  }

  /** A project with the given `scala.version` and the given scalac `<args>` on the scala-maven-plugin. */
  private def projectWithScalacArgs(scalaVersion: String, args: String*): MavenProject = {
    val config = new Xpp3Dom("configuration")
    val argsDom = new Xpp3Dom("args")
    args.foreach { arg =>
      val argDom = new Xpp3Dom("arg")
      argDom.setValue(arg)
      argsDom.addChild(argDom)
    }
    config.addChild(argsDom)

    val plugin = new Plugin()
    plugin.setGroupId("net.alchim31.maven")
    plugin.setArtifactId("scala-maven-plugin")
    plugin.setConfiguration(config)

    val build = new Build()
    build.addPlugin(plugin)
    val model = new Model()
    model.setBuild(build)
    val project = new MavenProject(model)
    project.getProperties().setProperty("scala.version", scalaVersion)
    project
  }
}
