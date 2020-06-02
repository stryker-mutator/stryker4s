import Release._
import sbt.Keys._
import sbt.ScriptedPlugin.autoImport.{scriptedBufferLog, scriptedLaunchOpts}
import sbt._

object Settings {
  lazy val commonSettings: Seq[Setting[_]] = Seq(
    Test / parallelExecution := false, // For logging tests
    addCompilerPlugin("com.olegpy" %% "better-monadic-for" % "0.3.1")
  )

  lazy val coreSettings: Seq[Setting[_]] = Seq(
    libraryDependencies ++= Seq(
      Dependencies.test.scalatest,
      Dependencies.test.mockitoScala,
      Dependencies.pureconfig,
      Dependencies.scalameta,
      Dependencies.betterFiles,
      Dependencies.log4jApi,
      Dependencies.log4jCore,
      Dependencies.grizzledSlf4j,
      Dependencies.log4jslf4jImpl % Test, // Logging tests need a slf4j implementation
      Dependencies.circeCore,
      Dependencies.sttpCirce,
      Dependencies.sttpCatsBackend,
      Dependencies.mutationTestingElements,
      Dependencies.mutationTestingMetrics,
      Dependencies.fs2Core,
      Dependencies.fs2IO
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

  lazy val sbtTestrunnerSettings: Seq[Setting[_]] = Seq(
    Test / parallelExecution := true, // No logging tests, so parallel can be true
    libraryDependencies ++= Seq(
      Dependencies.testInterface
    )
  )

  lazy val apiSettings: Seq[Setting[_]] = Seq(
    Test / parallelExecution := true // No logging tests, so parallel can be true
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
}
