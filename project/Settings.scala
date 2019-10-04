import Release._
import sbt.Keys._
import sbt.ScriptedPlugin.autoImport.{scriptedBufferLog, scriptedLaunchOpts}
import sbt._

object Settings {

  lazy val scalacOpts: Seq[String] = Seq(
    "-deprecation", // Emit warning and location for usages of deprecated APIs.
    "-encoding",
    "utf-8", // Specify character encoding used by source files.
    "-explaintypes", // Explain type errors in more detail.
    "-feature", // Emit warning and location for usages of features that should be imported explicitly.
    "-unchecked", // Enable additional warnings where generated code depends on assumptions.
    "-Xfatal-warnings", // Fail the compilation if there are any warnings.
    "-Xlint:doc-detached", // A Scaladoc comment appears to be detached from its element.
    "-Ywarn-infer-any", // Warn when a type argument is inferred to be `Any`.
    "-Ywarn-dead-code", // Warn when dead code is identified.
    "-Ywarn-unused:implicits", // Warn if an implicit parameter is unused.
    "-Ywarn-unused:imports", // Warn if an import selector is not referenced.
    "-Ywarn-unused:params", // Warn if a value parameter is unused.
    "-Ywarn-unused:patvars" // Warn if a variable bound in a pattern is unused.
  )

  lazy val commonSettings: Seq[Setting[_]] = Seq(
    Test / parallelExecution := false // For logging tests
  )

  lazy val coreSettings: Seq[Setting[_]] = Seq(
    resolvers += "jitpack" at "https://jitpack.io",
    libraryDependencies ++= Seq(
      Dependencies.test.scalatest,
      Dependencies.test.everitJsonSchema,
      Dependencies.test.mockitoScala,
      Dependencies.test.mutationTestingSchema,
      Dependencies.pureconfig,
      Dependencies.scalameta,
      Dependencies.betterFiles,
      Dependencies.log4jApi,
      Dependencies.log4jCore,
      Dependencies.grizzledSlf4j,
      Dependencies.log4jslf4jImpl % Test, // Logging tests need a slf4j implementation
      Dependencies.circeCore,
      Dependencies.circeGeneric,
      Dependencies.scalajHttp,
      Dependencies.mutationTestingElements
    )
  )

  lazy val commandRunnerSettings: Seq[Setting[_]] = Seq(
    libraryDependencies ++= Seq(
      Dependencies.log4jslf4jImpl,
      Dependencies.test.scalatest
    )
  )

  lazy val sbtPluginSettings: Seq[Setting[_]] = Seq(
    scriptedLaunchOpts := {
      scriptedLaunchOpts.value ++
        Seq("-Xmx1024M", "-Dplugin.version=" + version.value)
    },
    scriptedBufferLog := false
  )

  lazy val buildLevelSettings: Seq[Setting[_]] = inThisBuild(
    releaseCommands ++
      buildInfo ++
      Seq(
        scalaVersion := Dependencies.versions.scala212,
        scalacOptions ++= Settings.scalacOpts
      )
  )

  lazy val buildInfo: Seq[Def.Setting[_]] = Seq(
    description := "Stryker4s, the mutation testing framework for Scala.",
    organization := "io.stryker-mutator",
    organizationHomepage := Some(url("https://stryker-mutator.io/")),
    homepage := Some(url("https://stryker-mutator.io/")),
    licenses := Seq("Apache-2.0" -> url("https://github.com/stryker-mutator/stryker4s/blob/master/LICENSE")),
    scmInfo := Some(
      ScmInfo(url("https://github.com/stryker-mutator/stryker4s"), "scm:git@github.com:stryker-mutator/stryker4s.git")
    ),
    developers := List(
      Developer("legopiraat", "Legopiraat", "", url("https://github.com/legopiraat")),
      Developer("hugo-vrijswijk", "Hugo", "", url("https://github.com/hugo-vrijswijk"))
    )
  )
}
