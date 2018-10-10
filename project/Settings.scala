import sbt.Keys._
import sbt._

object Settings {
  val scalacOpts = Seq(
    "-deprecation", // Emit warning and location for usages of deprecated APIs.
    "-encoding",
    "utf-8", // Specify character encoding used by source files.
    "-explaintypes", // Explain type errors in more detail.
    "-feature", // Emit warning and location for usages of features that should be imported explicitly.
    "-unchecked", // Enable additional warnings where generated code depends on assumptions.
    "-Xfatal-warnings", // Fail the compilation if there are any warnings.
    "-Xlint:doc-detached", // A Scaladoc comment appears to be detached from its element.
    "-Ywarn-infer-any", // Warn when a type argument is inferred to be `Any`.
    "-Ywarn-dead-code" // Warn when dead code is identified.
  )

  def commonSettings: Seq[Setting[_]] = Seq(
    Test / parallelExecution := false, // For logging tests
    libraryDependencies ++= Seq(
      Dependencies.test.scalatest,
      Dependencies.test.mockitoScala,
      Dependencies.pureconfig,
      Dependencies.scalameta,
      Dependencies.scalametaContrib,
      Dependencies.betterFiles,
      Dependencies.log4jApi,
      Dependencies.log4jCore,
      Dependencies.grizzledSlf4j
    )
  )

  def coreSettings: Seq[Setting[_]] = Seq(
    libraryDependencies ++= Seq(
      Dependencies.log4jslf4jImpl
    )
  )

  def buildLevelSettings: Seq[Setting[_]] = {
    inThisBuild(
      Seq(
        name := "stryker4s",
        version := "0.0.1",
        description := "Stryker4s the mutation testing framework for Scala.",
        organization := "io.stryker-mutator",
        organizationHomepage := Some(url("https://stryker-mutator.io/")),
        crossScalaVersions := Dependencies.versions.crossScala,
        scalacOptions ++= Settings.scalacOpts,
      ))
  }
}
