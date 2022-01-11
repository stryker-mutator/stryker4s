import Release.*
import sbt.Keys.*
import sbt.ScriptedPlugin.autoImport.{scriptedBufferLog, scriptedLaunchOpts}
import sbt.*
import sbtprotoc.ProtocPlugin.autoImport.PB

object Settings {
  lazy val commonSettings: Seq[Setting[?]] = Seq(
    libraryDependencies ++= (CrossVersion.partialVersion(scalaVersion.value) match {
      case Some((2, _)) =>
        Seq(compilerPlugin("com.olegpy" %% "better-monadic-for" % "0.3.1"))
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
    // Fatal warnings only in CI
    scalacOptions --= (if (sys.env.exists { case (k, v) => k == "CI" && v == "true" }) Nil
                       else Seq("-Xfatal-warnings")),
    scalacOptions ++= (CrossVersion.partialVersion(scalaVersion.value) match {
      case Some((2, _)) => Seq("-Xsource:3")
      case _            => Seq.empty
    })
  )

  lazy val coreSettings: Seq[Setting[?]] = Seq(
    libraryDependencies ++= Seq(
      Dependencies.catsCore,
      Dependencies.catsEffect,
      Dependencies.circeCore,
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
    scriptedBufferLog := false,
    // If you build and publish a plugin using sbt 1.4.0, your users will also be forced to upgrade to sbt 1.4.0 immediately. To prevent this you can cross build your plugin against sbt 1.2.8 (while using sbt 1.4.0) as follows:
    pluginCrossBuild / sbtVersion := "1.2.8"
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
    // Prevent version clash warnings when running Stryker4s on a locally-published on Stryker4s
    libraryDependencySchemes ++= Seq(
      "io.stryker-mutator" %% "stryker4s-core" % VersionScheme.Always,
      "io.stryker-mutator" %% "stryker4s-api" % VersionScheme.Always,
      "io.stryker-mutator" %% "sbt-stryker4s-testrunner" % VersionScheme.Always
    ),
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
      Developer("hugo-vrijswijk", "Hugo", "", url("https://github.com/hugo-vrijswijk"))
    ),
    versionScheme := Some("semver-spec")
  )
}
