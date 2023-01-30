import io.github.davidgregory084.*
import Release.*
import io.github.davidgregory084.TpolecatPlugin.autoImport.*
import sbt.Keys.*
import sbt.ScriptedPlugin.autoImport.{scriptedBufferLog, scriptedLaunchOpts}
import sbt.*
import sbtprotoc.ProtocPlugin.autoImport.PB
import scoverage.ScoverageKeys.*

object Settings {
  lazy val commonSettings: Seq[Setting[?]] = Seq(
    libraryDependencies ++= (CrossVersion.partialVersion(scalaVersion.value) match {
      case Some((2, _)) =>
        Seq(
          compilerPlugin("com.olegpy" %% "better-monadic-for" % "0.3.1"),
          compilerPlugin("org.typelevel" % "kind-projector" % "0.13.2" cross CrossVersion.full)
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
    scalacOptions ++= Seq("-unchecked"),
    tpolecatScalacOptions += ScalacOption("-Xsource:3", _.major == 2)
  )

  lazy val coreSettings: Seq[Setting[?]] = Seq(
    libraryDependencies ++= Seq(
      Dependencies.catsCore,
      Dependencies.catsEffect,
      Dependencies.circeCore,
      Dependencies.fansi,
      Dependencies.fs2Core,
      Dependencies.fs2IO,
      Dependencies.mutationTestingElements,
      Dependencies.mutationTestingMetrics,
      Dependencies.pureconfig,
      Dependencies.pureconfigSttp,
      Dependencies.scalameta,
      Dependencies.sttpCirce,
      Dependencies.sttpFs2Backend,
      Dependencies.weaponRegeX,
      Dependencies.test.catsEffectScalaTest,
      Dependencies.test.mockitoScala,
      Dependencies.test.mockitoScalaCats,
      Dependencies.test.scalatest
    )
  )

  lazy val jvmSettings: Seq[Setting[?]] = Seq(
    libraryDependencies ++= Seq(
      Dependencies.test.catsEffectScalaTest,
      Dependencies.test.mockitoScala,
      Dependencies.test.mockitoScalaCats,
      Dependencies.test.scalatest
    )
  )
  lazy val jvmMutatorScalaSettings: Seq[Setting[?]] = Seq(
    libraryDependencies ++= Seq(
      Dependencies.catsCore,
      Dependencies.catsEffect,
      Dependencies.circeCore,
      Dependencies.fansi,
      Dependencies.fs2Core,
      Dependencies.fs2IO,
      Dependencies.mutationTestingElements,
      Dependencies.mutationTestingMetrics,
      Dependencies.pureconfig,
      Dependencies.pureconfigSttp,
      Dependencies.scalameta,
      Dependencies.sttpCirce,
      Dependencies.sttpFs2Backend,
      Dependencies.weaponRegeX,
      Dependencies.test.catsEffectScalaTest,
      Dependencies.test.mockitoScala,
      Dependencies.test.mockitoScalaCats,
      Dependencies.test.scalatest
    )
  )

  lazy val commandRunnerSettings: Seq[Setting[?]] = Seq(
    libraryDependencies ++= Seq(
      Dependencies.slf4j,
      Dependencies.test.scalatest
    )
  )

  lazy val sbtPluginSettings: Seq[Setting[?]] = Seq(
    scriptedLaunchOpts := {
      scriptedLaunchOpts.value ++
        Seq("-Xmx1024M", "-Dplugin.version=" + version.value)
    },
    scriptedBufferLog := false
  )

  lazy val sbtTestrunnerSettings: Seq[Setting[?]] = Seq(
    libraryDependencies ++= Seq(
      Dependencies.testInterface
    )
  )

  lazy val apiSettings: Seq[Setting[?]] = Seq(
    Compile / PB.targets := Seq(
      scalapb.gen(grpc = false, lenses = false) -> (Compile / sourceManaged).value / "scalapb"
    ),
    libraryDependencies += Dependencies.scalapbRuntime
  )

  lazy val buildLevelSettings: Seq[Setting[?]] = inThisBuild(
    releaseCommands ++
      buildInfo
  )

  lazy val buildInfo: Seq[Def.Setting[?]] = Seq(
    tpolecatReleaseModeEnvVar := "CI_RELEASE",
    tpolecatDefaultOptionsMode := DevMode,
    // Prevent version clash warnings when running Stryker4s on a locally-published on Stryker4s
    libraryDependencySchemes ++= Seq(
      "io.stryker-mutator" %% "stryker4jvm-api" % VersionScheme.Always,
      "io.stryker-mutator" %% "stryker4jvm" % VersionScheme.Always,
      "io.stryker-mutator" %% "stryker4jvm-mutator-scala" % VersionScheme.Always,
      "io.stryker-mutator" %% "sbt-stryker4s-testrunner" % VersionScheme.Always
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
    versionScheme := Some("semver-spec"),

    // scoverage settings
    coverageExcludedPackages := ".*stryker4jvm\\.api.*;.*stryker4jvm\\.plugin.*;.*stryker4jvm\\.coverage.*;.*stryker4jvm\\.command.*",
    coverageExcludedFiles := "stryker4jvm\\.package",
    coverageFailOnMinimum := true,
    coverageMinimumStmtTotal := 70
  )
}
