package stryker4s.maven.runner

import org.apache.maven.project.MavenProject
import stryker4s.exception.TestSetupFailedException

import scala.jdk.CollectionConverters.*

/** Helpers to determine the target project's Scala version and binary (artifact-suffix) version. */
object ScalaVersions {

  /** The binary version used as the artifact cross-suffix: `3` for Scala 3, `2.13`/`2.12` for Scala 2. */
  def binaryVersion(fullVersion: String): String =
    fullVersion.split('.') match {
      case Array("3", _*)          => "3"
      case Array(major, minor, _*) => s"$major.$minor"
      case _                       => fullVersion
    }

  /** The full Scala version of the project, from the `scala.version` property or, failing that, the resolved
    * `scala-library` / `scala3-library_3` dependency.
    */
  def fullVersionUnsafe(project: MavenProject): String =
    fullVersion(project).getOrElse(
      throw TestSetupFailedException(
        "Could not determine the project's Scala version. Set the 'scala.version' property in your pom.xml."
      )
    )

  /** Like [[fullVersion]], but `None` instead of throwing when the version cannot be determined. */
  def fullVersion(project: MavenProject): Option[String] =
    Option(project.getProperties().getProperty("scala.version")).orElse(detectFromDependencies(project))

  /** The `<args>` configured on the scala-maven-plugin (the project's scalac options). */
  def scalacOptions(project: MavenProject): Seq[String] =
    Option(project.getPlugin("net.alchim31.maven:scala-maven-plugin"))
      .flatMap(plugin => Option(plugin.getConfiguration()))
      .collect { case dom: org.codehaus.plexus.util.xml.Xpp3Dom => dom }
      .flatMap(dom => Option(dom.getChild("args")))
      .toSeq
      .flatMap(_.getChildren().toSeq)
      .map(_.getValue())
      .flatMap(Option(_))

  private def detectFromDependencies(project: MavenProject): Option[String] =
    project.getArtifacts().asScala.collectFirst {
      case a if a.getArtifactId() == "scala3-library_3" => a.getVersion()
      case a if a.getArtifactId() == "scala-library"    => a.getVersion()
    }
}
