package stryker4s.maven.runner

import org.apache.maven.model.Plugin
import org.apache.maven.project.MavenProject
import org.codehaus.plexus.util.xml.Xpp3Dom
import stryker4s.testkit.{LogMatchers, Stryker4sSuite}

class MavenTestSelectionTest extends Stryker4sSuite with LogMatchers {

  private val scalaTest = "org.scalatest.tools.Framework"
  private val specs2 = "org.specs2.runner.Specs2Framework"

  test("returns None when no understood test plugin is configured") {
    assertEquals(MavenTestSelection.fromProject(new MavenProject()), None)
  }

  test("an enabled scalatest-maven-plugin selects ScalaTest frameworks but not Surefire-driven ones") {
    val select = MavenTestSelection.fromProject(projectWith(scalatestPlugin())).value

    assert(select(scalaTest))
    // specs2/JUnit are driven by Surefire, which is not present here
    assert(!select(specs2))
    assertLoggedDebug("scalatest-maven-plugin is enabled.")
  }

  test("an enabled maven-surefire-plugin selects non-ScalaTest frameworks but not ScalaTest") {
    val select = MavenTestSelection.fromProject(projectWith(surefirePlugin())).value

    assert(select(specs2))
    assert(!select(scalaTest))
    assertLoggedDebug("maven-surefire-plugin is enabled.")
  }

  test("a skipped scalatest-maven-plugin is not enabled") {
    assertEquals(MavenTestSelection.fromProject(projectWith(scalatestPlugin("skipTests" -> "true"))), None)
  }

  test("a surefire plugin skipped via skipTests is not enabled") {
    assertEquals(MavenTestSelection.fromProject(projectWith(surefirePlugin("skipTests" -> "true"))), None)
    assertLoggedDebug("maven-surefire-plugin is skipped; not selecting its tests.")
  }

  test("a plugin skipped via the skip configuration is not enabled") {
    assertEquals(MavenTestSelection.fromProject(projectWith(surefirePlugin("skip" -> "true"))), None)
  }

  test("the skipTests property disables an otherwise-enabled plugin") {
    val project = projectWith(surefirePlugin())
    project.getProperties().setProperty("skipTests", "true")

    assertEquals(MavenTestSelection.fromProject(project), None)
  }

  test("the maven.test.skip property disables an otherwise-enabled plugin") {
    val project = projectWith(surefirePlugin())
    project.getProperties().setProperty("maven.test.skip", "true")

    assertEquals(MavenTestSelection.fromProject(project), None)
  }

  test("ScalaTest is selected while specs2 is excluded when Surefire is skipped (the archetype layout)") {
    val project = new MavenProject()
    project.getBuild().addPlugin(scalatestPlugin())
    project.getBuild().addPlugin(surefirePlugin("skipTests" -> "true"))
    val select = MavenTestSelection.fromProject(project).value

    assert(select(scalaTest))
    assert(!select(specs2))
  }

  private def scalatestPlugin(config: (String, String)*): Plugin =
    plugin("org.scalatest", "scalatest-maven-plugin", config*)

  private def surefirePlugin(config: (String, String)*): Plugin =
    plugin("org.apache.maven.plugins", "maven-surefire-plugin", config*)

  private def plugin(groupId: String, artifactId: String, config: (String, String)*): Plugin = {
    val p = new Plugin()
    p.setGroupId(groupId)
    p.setArtifactId(artifactId)
    if config.nonEmpty then {
      val dom = new Xpp3Dom("configuration")
      config.foreach { case (name, value) =>
        val child = new Xpp3Dom(name)
        child.setValue(value)
        dom.addChild(child)
      }
      p.setConfiguration(dom)
    }
    p
  }

  private def projectWith(plugin: Plugin): MavenProject = {
    val project = new MavenProject()
    project.getBuild().addPlugin(plugin)
    project
  }
}
