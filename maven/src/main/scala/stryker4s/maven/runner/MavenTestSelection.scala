package stryker4s.maven.runner

import org.apache.maven.model.{Plugin, PluginExecution}
import org.apache.maven.project.MavenProject
import org.codehaus.plexus.util.xml.Xpp3Dom
import stryker4s.log.Logger

import scala.jdk.CollectionConverters.*

/** Determines which discovered test frameworks the project's configured test plugins would run, so Stryker runs the
  * same frameworks as `mvn test` instead of every `sbt.testing` framework on the classpath.
  *
  * Maven has no single test abstraction; each test plugin drives specific frameworks. We only check whether the plugins
  * we understand are *enabled* (not skipped) and route each discovered framework to its driver — we deliberately do not
  * replicate per-class include/exclude/suite filtering:
  *   - `scalatest-maven-plugin` drives ScalaTest.
  *   - `maven-surefire-plugin` drives JUnit, specs2 (via specs2-junit), TestNG, …
  *
  * If neither plugin is enabled, discovery is left unrestricted (`None`), preserving the previous "run everything
  * discovered" behaviour.
  */
object MavenTestSelection {

  private val surefireKey = "org.apache.maven.plugins:maven-surefire-plugin"
  private val scalatestKey = "org.scalatest:scalatest-maven-plugin"

  /** `sbt.testing.Framework` implementations driven by the scalatest-maven-plugin (everything else goes via Surefire).
    */
  private val scalatestFrameworks = Set("org.scalatest.tools.Framework", "org.scalatest.tools.ScalaTestFramework")

  /** A predicate over a discovered framework's class name, or `None` when no understood test plugin is enabled (don't
    * restrict). A framework is selected when the plugin that drives it is enabled.
    */
  def fromProject(project: MavenProject)(using log: Logger): Option[String => Boolean] = {
    val scalatestEnabled = isEnabled(project, scalatestKey, "scalatest-maven-plugin")
    val surefireEnabled = isEnabled(project, surefireKey, "maven-surefire-plugin")

    Option.when(scalatestEnabled || surefireEnabled)(frameworkClass =>
      if scalatestFrameworks.contains(frameworkClass) then scalatestEnabled else surefireEnabled
    )
  }

  /** Whether a configured test plugin is present and not skipped (via `maven.test.skip`/`skipTests` properties or the
    * plugin's own `skipTests`/`skip` configuration).
    */
  private def isEnabled(project: MavenProject, key: String, name: String)(using log: Logger): Boolean =
    Option(project.getPlugin(key)).exists { plugin =>
      val cfg = configsOf(plugin)
      val skipped = boolProp(project, "maven.test.skip") || boolProp(project, "skipTests") ||
        boolConfig(cfg, "skipTests") || boolConfig(cfg, "skip")
      log.debug(if skipped then s"$name is skipped; not selecting its tests." else s"$name is enabled.")
      !skipped
    }

  /** Plugin-level configuration plus every execution's configuration (test plugins often configure at execution level).
    */
  private def configsOf(plugin: Plugin): Seq[Xpp3Dom] =
    (Option(plugin.getConfiguration()).toSeq ++
      plugin.getExecutions().asScala.toSeq.flatMap((e: PluginExecution) => Option(e.getConfiguration())))
      .collect { case dom: Xpp3Dom => dom }

  private def boolProp(project: MavenProject, name: String): Boolean =
    Option(project.getProperties().getProperty(name)).exists(_.trim.equalsIgnoreCase("true"))

  private def boolConfig(configs: Seq[Xpp3Dom], name: String): Boolean =
    configs
      .flatMap(cfg => Option(cfg.getChild(name)))
      .flatMap(node => Option(node.getValue()))
      .exists(
        _.trim.equalsIgnoreCase("true")
      )
}
