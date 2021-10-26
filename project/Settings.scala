import Release._
import sbt.Keys._
import sbt.ScriptedPlugin.autoImport.{scriptedBufferLog, scriptedLaunchOpts}
import sbt._
import sbtprotoc.ProtocPlugin.autoImport.PB
import scalajsbundler.sbtplugin.ScalaJSBundlerPlugin.autoImport._
import org.scalablytyped.converter.plugin.ScalablyTypedPluginBase.autoImport._
import org.scalablytyped.converter.plugin.ScalablyTypedConverterGenSourcePlugin.autoImport._
object Settings {
  lazy val commonSettings: Seq[Setting[_]] = Seq(
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
                       else Seq("-Xfatal-warnings"))
  )

  lazy val coreSettings: Seq[Setting[_]] = Seq(
    libraryDependencies ++= Seq(
      Dependencies.catsCore.value,
      Dependencies.catsEffect.value,
      Dependencies.circeCore.value,
      Dependencies.fs2Core.value,
      Dependencies.fs2IO.value,
      Dependencies.mutationTestingElements,
      Dependencies.mutationTestingMetrics.value,
      Dependencies.scalameta.value,
      Dependencies.sttpCirce.value,
      Dependencies.weaponRegeX.value,
      Dependencies.test.catsEffectScalaTest.value,
      Dependencies.test.scalatest.value
    )
  )

  lazy val coreJVMSettings: Seq[Setting[_]] = Seq(
    libraryDependencies ++= Seq(
      Dependencies.test.mockitoScala,
      Dependencies.test.mockitoScalaCats,
      Dependencies.pureconfig,
      Dependencies.pureconfigSttp,
      Dependencies.sttpFs2Backend
    )
  )

  lazy val coreJSettings: Seq[Setting[_]] = Seq(
    libraryDependencies ++= Seq(
      Dependencies.sttpCatsBackend.value
    ),
    stOutputPackage := "stryker4s.st",
    // stPrivateWithin := Some("stryker4s"),
    Compile / npmDependencies ++= Seq(
      "minimatch" -> "3.0.4",
      "@types/minimatch" -> "3.0.5"
    )
  )

  lazy val commandRunnerSettings: Seq[Setting[_]] = Seq(
    libraryDependencies ++= Seq(
      Dependencies.log4j,
      Dependencies.test.scalatest.value
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
    )
  )

  lazy val apiSettings: Seq[Setting[_]] = Seq(
    Compile / PB.targets := Seq(
      scalapb.gen(grpc = false, lenses = false) -> (Compile / sourceManaged).value / "scalapb"
    ),
    libraryDependencies += Dependencies.scalapbRuntime.value
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
      Developer("hugo-vrijswijk", "Hugo", "", url("https://github.com/hugo-vrijswijk"))
    ),
    versionScheme := Some("semver-spec")
  )
}
