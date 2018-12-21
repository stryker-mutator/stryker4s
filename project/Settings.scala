import sbt.Keys._
import sbt._
import scoverage.ScoverageKeys._
import xerial.sbt.Sonatype.autoImport.sonatypePublishTo

object Settings {
  val scalacOpts: Seq[String] = Seq(
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
    publishTo := sonatypePublishTo.value,
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

  def buildLevelSettings: Seq[Setting[_]] = inThisBuild(
    buildInfo ++
      Seq(
        scalaVersion := Dependencies.versions.scala212,
        scalacOptions ++= Settings.scalacOpts,
        coverageMinimum := 75
      )
  )

  lazy val buildInfo: Seq[Def.Setting[_]] = Seq(
    name := "stryker4s",
    version := "0.1.0",
    description := "Stryker4s, the mutation testing framework for Scala.",
    organization := "io.stryker-mutator",
    organizationHomepage := Some(url("https://stryker-mutator.io/")),
    homepage := Some(url("https://stryker-mutator.io/")),
    licenses := Seq(
      "Apache-2.0" -> url("https://github.com/stryker-mutator/stryker4s/blob/master/LICENSE")),
    scmInfo := Some(
      ScmInfo(url("https://github.com/stryker-mutator/stryker4s"),
              "scm:git@github.com:stryker-mutator/stryker4s.git")),
    developers := List(
      Developer("legopiraat", "Legopiraat", "", url("https://github.com/legopiraat")),
      Developer("hugo-vrijswijk", "Hugo", "", url("https://github.com/hugo-vrijswijk"))
    ),
  )
}
