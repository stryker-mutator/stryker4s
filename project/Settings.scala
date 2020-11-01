import Release._
import sbt.Keys._
import sbt.ScriptedPlugin.autoImport.{scriptedBufferLog, scriptedLaunchOpts}
import sbt._
import dotty.tools.sbtplugin.DottyPlugin.autoImport.isDotty

object Settings {
  lazy val commonSettings: Seq[Setting[_]] = Seq(
    libraryDependencies ++= (if (!isDotty.value) Seq(compilerPlugin("com.olegpy" %% "better-monadic-for" % "0.3.1"))
                             else Nil)
  )

  lazy val coreSettings: Seq[Setting[_]] = Seq(
    libraryDependencies ++= Seq(
      Dependencies.test.scalatest,
      Dependencies.test.mockitoScala,
      Dependencies.test.catsEffectScalaTest,
      Dependencies.pureconfig,
      Dependencies.pureconfigSttp,
      Dependencies.scalameta,
      Dependencies.betterFiles,
      Dependencies.circeCore,
      Dependencies.sttpCirce,
      Dependencies.sttpCatsBackend,
      Dependencies.mutationTestingElements,
      Dependencies.mutationTestingMetrics,
      Dependencies.catsCore,
      Dependencies.catsEffect,
      Dependencies.fs2Core,
      Dependencies.fs2IO
    )
  )

  lazy val commandRunnerSettings: Seq[Setting[_]] = Seq(
    libraryDependencies ++= Seq(
      Dependencies.log4j,
      Dependencies.test.scalatest
    )
  )

  lazy val sbtPluginSettings: Seq[Setting[_]] = Seq(
    scriptedLaunchOpts := {
      scriptedLaunchOpts.value ++
        Seq("-Xmx1024M", "-Dplugin.version=" + version.value)
    },
    scriptedBufferLog := false,
    // If you build and publish a plugin using sbt 1.4.0, your users will also be forced to upgrade to sbt 1.4.0 immediately. To prevent this you can cross build your plugin against sbt 1.2.8 (while using sbt 1.4.0) as follows:
    pluginCrossBuild / sbtVersion := "1.2.8"
  )

  lazy val sbtTestrunnerSettings: Seq[Setting[_]] = Seq(
    libraryDependencies ++= Seq(
      Dependencies.testInterface
    ),
    scalacOptions in (Compile, doc) := filterDottyDocScalacOptions.value
  )

  lazy val apiSettings: Seq[Setting[_]] = Seq(
    scalacOptions in (Compile, doc) := filterDottyDocScalacOptions.value
  )

  lazy val buildLevelSettings: Seq[Setting[_]] = inThisBuild(
    releaseCommands ++
      buildInfo
  )

  lazy val buildInfo: Seq[Def.Setting[_]] = Seq(
    description := "Stryker4s, the mutation testing framework for Scala.",
    organization := "io.stryker-mutator",
    organizationHomepage := Some(url("https://stryker-mutator.io/")),
    homepage := Some(url("https://stryker-mutator.io/")),
    licenses := Seq("Apache-2.0" -> url("https://github.com/stryker-mutator/stryker4s/blob/master/LICENSE")),
    scmInfo := Some(
      ScmInfo(
        url("https://github.com/stryker-mutator/stryker4s"),
        "scm:git:https://github.com/stryker-mutator/stryker4s.git",
        "scm:git:git@github.com:stryker-mutator/stryker4s.git"
      )
    ),
    developers := List(
      Developer("legopiraat", "Legopiraat", "", url("https://github.com/legopiraat")),
      Developer("hugo-vrijswijk", "Hugo", "", url("https://github.com/hugo-vrijswijk"))
    )
  )

  // Dotty doc generation creates warnings. Ignore them for now
  val filterDottyDocScalacOptions = Def.task {
    val options = (scalacOptions in (Compile, doc)).value
    if (isDotty.value) options.filterNot(_ == "-Xfatal-warnings")
    else options
  }
}
