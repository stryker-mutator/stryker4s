package stryker4s.maven.runner

import org.apache.maven.artifact.DefaultArtifact
import org.apache.maven.artifact.handler.DefaultArtifactHandler
import org.apache.maven.model.{Build, Model, Plugin}
import org.apache.maven.project.MavenProject
import org.codehaus.plexus.util.xml.Xpp3Dom
import stryker4s.exception.TestSetupFailedException
import stryker4s.testkit.Stryker4sSuite

import scala.jdk.CollectionConverters.*

class ScalaVersionsTest extends Stryker4sSuite {
  test("binaryVersion of a Scala 3 version is 3") {
    assertEquals(ScalaVersions.binaryVersion("3.3.4"), "3")
  }

  test("binaryVersion of a Scala 3 RC is 3") {
    assertEquals(ScalaVersions.binaryVersion("3.9.0-RC1"), "3")
  }

  test("binaryVersion of a Scala 2.13 version is 2.13") {
    assertEquals(ScalaVersions.binaryVersion("2.13.18"), "2.13")
  }

  test("binaryVersion of a Scala 2.12 version is 2.12") {
    assertEquals(ScalaVersions.binaryVersion("2.12.20"), "2.12")
  }

  test("binaryVersion falls back to the full version when it has no minor part") {
    assertEquals(ScalaVersions.binaryVersion("2"), "2")
  }

  test("fullVersion reads the scala.version property") {
    val project = new MavenProject()
    project.getProperties().setProperty("scala.version", "3.3.4")

    assertEquals(ScalaVersions.fullVersion(project), Some("3.3.4"))
  }

  test("fullVersion detects the version from the scala3-library_3 dependency") {
    val project = new MavenProject()
    project.setArtifacts(Set(artifact("scala3-library_3", "3.3.4")).asJava)

    assertEquals(ScalaVersions.fullVersion(project), Some("3.3.4"))
  }

  test("fullVersion detects the version from the scala-library dependency") {
    val project = new MavenProject()
    project.setArtifacts(Set(artifact("scala-library", "2.13.18")).asJava)

    assertEquals(ScalaVersions.fullVersion(project), Some("2.13.18"))
  }

  test("fullVersion ignores unrelated dependencies") {
    val project = new MavenProject()
    project.setArtifacts(Set(artifact("cats-core_3", "2.12.0")).asJava)

    assertEquals(ScalaVersions.fullVersion(project), None)
  }

  test("fullVersion prefers the scala.version property over dependencies") {
    val project = new MavenProject()
    project.getProperties().setProperty("scala.version", "3.3.4")
    project.setArtifacts(Set(artifact("scala-library", "2.13.18")).asJava)

    assertEquals(ScalaVersions.fullVersion(project), Some("3.3.4"))
  }

  test("fullVersionUnsafe returns the version when it can be determined") {
    val project = new MavenProject()
    project.getProperties().setProperty("scala.version", "3.3.4")

    assertEquals(ScalaVersions.fullVersionUnsafe(project), "3.3.4")
  }

  test("fullVersionUnsafe throws a TestSetupFailedException when the version cannot be determined") {
    val e = intercept[TestSetupFailedException](ScalaVersions.fullVersionUnsafe(new MavenProject()))
    assert(
      e.getMessage().contains("Could not determine the project's Scala version"),
      s"Unexpected message: ${e.getMessage()}"
    )
  }

  test("scalacOptions reads the <args> of the scala-maven-plugin configuration") {
    val project = projectWithScalacArgs("-Xsource:3", "-deprecation")

    assertEquals(ScalaVersions.scalacOptions(project), Seq("-Xsource:3", "-deprecation"))
  }

  test("scalacOptions is empty when the scala-maven-plugin is not configured") {
    assertEquals(ScalaVersions.scalacOptions(new MavenProject()), Seq.empty)
  }

  test("scalacOptions is empty when there is no <args> element") {
    val plugin = new Plugin()
    plugin.setGroupId("net.alchim31.maven")
    plugin.setArtifactId("scala-maven-plugin")
    plugin.setConfiguration(new Xpp3Dom("configuration"))

    assertEquals(ScalaVersions.scalacOptions(projectWith(plugin)), Seq.empty)
  }

  test("scalacOptions ignores a plugin with a different coordinate") {
    val plugin = new Plugin()
    plugin.setGroupId("net.alchim31.maven")
    plugin.setArtifactId("some-other-plugin")
    plugin.setConfiguration(scalacArgsConfig("-Xsource:3"))

    assertEquals(ScalaVersions.scalacOptions(projectWith(plugin)), Seq.empty)
  }

  private def artifact(artifactId: String, version: String): DefaultArtifact =
    new DefaultArtifact(
      "org.scala-lang",
      artifactId,
      version,
      "compile",
      "jar",
      "",
      new DefaultArtifactHandler("jar")
    )

  private def scalacArgsConfig(args: String*): Xpp3Dom = {
    val config = new Xpp3Dom("configuration")
    val argsDom = new Xpp3Dom("args")
    args.foreach { arg =>
      val argDom = new Xpp3Dom("arg")
      argDom.setValue(arg)
      argsDom.addChild(argDom)
    }
    config.addChild(argsDom)
    config
  }

  private def projectWithScalacArgs(args: String*): MavenProject = {
    val plugin = new Plugin()
    plugin.setGroupId("net.alchim31.maven")
    plugin.setArtifactId("scala-maven-plugin")
    plugin.setConfiguration(scalacArgsConfig(args*))
    projectWith(plugin)
  }

  private def projectWith(plugin: Plugin): MavenProject = {
    val build = new Build()
    build.addPlugin(plugin)
    val model = new Model()
    model.setBuild(build)
    new MavenProject(model)
  }
}
