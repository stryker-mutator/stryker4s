import Release.*
import org.typelevel.sbt.tpolecat.*
import org.typelevel.scalacoptions.*
import sbt.*
import sbt.Keys.*
import sbt.ScriptedPlugin.autoImport.{scriptedBufferLog, scriptedLaunchOpts}
import sbtprotoc.ProtocPlugin.autoImport.PB

import TpolecatPlugin.autoImport.*

object Settings {
  lazy val commonSettings: Seq[Setting[?]] = Seq(
    libraryDependencies ++= (CrossVersion.partialVersion(scalaVersion.value) match {
      case Some((2, _)) =>
        Seq(
          compilerPlugin("com.olegpy" %% "better-monadic-for" % "0.3.1"),
          compilerPlugin("org.typelevel" % "kind-projector" % "0.13.3" cross CrossVersion.full)
        )
      case _ =>
        Nil
    }),
    // Add src/main/ scala-2.13- and scala-2.13+ source directories
    Compile / unmanagedSourceDirectories += {
      val sourceDir = (Compile / sourceDirectory).value
      CrossVersion.partialVersion(scalaVersion.value) match {
        case Some((2, n)) if n <= 12 => sourceDir / "scala-2.13-"
        case _                       => sourceDir / "scala-2.13+"
      }
    },
    tpolecatScalacOptions ++= Set(ScalacOptions.source3, ScalacOptions.release("11"))
  )

  lazy val coreSettings: Seq[Setting[?]] = Seq(
    moduleName := "stryker4s-core",
    libraryDependencies ++= Seq(
      Dependencies.catsCore,
      Dependencies.catsEffect,
      Dependencies.circeCore,
      Dependencies.ciris,
      Dependencies.decline,
      Dependencies.fansi,
      Dependencies.fs2Core,
      Dependencies.fs2IO,
      Dependencies.hocon,
      Dependencies.mutationTestingElements,
      Dependencies.mutationTestingMetrics,
      Dependencies.scalameta,
      Dependencies.sttpCirce,
      Dependencies.sttpFs2Backend,
      Dependencies.weaponRegeX
    ),
    // Scaladoc is buggy on scala 3, so we disable it
    Compile / doc / sources := (CrossVersion.partialVersion(scalaVersion.value) match {
      case Some((3, _)) => Seq.empty
      case _            => (Compile / doc / sources).value
    })
  )

  lazy val commandRunnerSettings: Seq[Setting[?]] = Seq(
    moduleName := "stryker4s-command-runner",
    libraryDependencies ++= Seq(
      Dependencies.slf4j
    )
  )

  lazy val sbtPluginSettings: Seq[Setting[?]] = Seq(
    moduleName := "sbt-stryker4s",
    scriptedLaunchOpts :=
      scriptedLaunchOpts.value ++
        Seq("-Xmx1024M", "-Dplugin.version=" + version.value),
    (pluginCrossBuild / sbtVersion) := {
      scalaBinaryVersion.value match {
        case "2.12" => "1.7.0"
        case _      => "2.0.0-M4"
      }
    },
    scriptedBufferLog := false
  )

  lazy val sbtTestRunnerSettings: Seq[Setting[?]] = Seq(
    moduleName := "stryker4s-sbt-testrunner",
    libraryDependencies ++= Seq(
      Dependencies.testInterface
    )
  )

  lazy val testRunnerApiSettings: Seq[Setting[?]] = Seq(
    moduleName := "stryker4s-testrunner-api",
    Compile / PB.targets := Seq(
      scalapb.gen(
        grpc = false,
        lenses = false,
        scala3Sources = scalaBinaryVersion.value == "3"
      ) -> (Compile / sourceManaged).value / "scalapb"
    ),
    libraryDependencies += Dependencies.scalapbRuntime,
    // Disable warnings for discarded non-Unit value results, as they are used in the generated code
    Compile / tpolecatExcludeOptions += ScalacOptions.warnValueDiscard
  )

  lazy val apiSettings: Seq[Setting[?]] = Seq(
    moduleName := "stryker4s-api",
    libraryDependencies ++= Seq(
      Dependencies.fansi
    )
  )

  lazy val testkitSettings: Seq[Setting[?]] = Seq(
    moduleName := "stryker4s-testkit",
    libraryDependencies ++= Seq(
      Dependencies.fansi,
      Dependencies.fs2IO,
      Dependencies.scalameta,
      Dependencies.test.munit,
      Dependencies.test.munitCatsEffect
    )
  )

  lazy val buildLevelSettings: Seq[Setting[?]] = inThisBuild(
    releaseCommands ++
      buildInfo
  )

  lazy val buildInfo: Seq[Def.Setting[?]] = Seq(
    // Fatal warnings only in CI
    tpolecatCiModeEnvVar := "CI",
    tpolecatDefaultOptionsMode := DevMode,
    // Prevent version clash warnings when running Stryker4s on a locally-published on Stryker4s
    libraryDependencySchemes ++= Seq(
      "io.stryker-mutator" %% "stryker4s-core" % VersionScheme.Always,
      "io.stryker-mutator" %% "stryker4s-testrunner-api" % VersionScheme.Always,
      "io.stryker-mutator" %% "stryker4s-sbt-testrunner" % VersionScheme.Always
    ),
    description := "Stryker4s, the mutation testing framework for Scala.",
    organization := "io.stryker-mutator",
    organizationHomepage := Some(url("https://stryker-mutator.io/")),
    homepage := Some(url("https://stryker-mutator.io/")),
    licenses := Seq(License.Apache2),
    scmInfo := Some(
      ScmInfo(
        url("https://github.com/stryker-mutator/stryker4s"),
        "scm:git:https://github.com/stryker-mutator/stryker4s.git",
        "scm:git:git@github.com:stryker-mutator/stryker4s.git"
      )
    ),
    developers := List(
      Developer("hugo-vrijswijk", "Hugo", "", url("https://github.com/hugo-vrijswijk"))
    ),
    versionScheme := Some("semver-spec")
  )
}
