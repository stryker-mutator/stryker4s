import Dependencies.*
import MillScripted.*
import Settings.*
import sbt.internal.ProjectMatrix

lazy val root = (project withId "stryker4s" in file("."))
  .settings(
    buildLevelSettings,
    publish / skip := true,
    // Publish locally for sbt plugin testing
    addCommandAlias(
      "publishPluginLocal",
      "set ThisBuild / version := \"0.0.0-TEST-SNAPSHOT\"; sbtPlugin/publishLocal; sbtPlugin3/publishLocal"
    ),
    // Publish to .m2 folder for Maven plugin testing
    addCommandAlias(
      "publishM2Local",
      "set ThisBuild / version := \"SET-BY-SBT-SNAPSHOT\"; core3/publishM2; testkit3/publishM2"
    ),
    // Publish to .ivy folder for command runner local testing
    addCommandAlias(
      "publishCommandRunnerLocal",
      "set ThisBuild / version := \"0.0.0-TEST-SNAPSHOT\"; commandRunner/publishLocal"
    ),
    // Publish Mill plugin + test runner for Mill plugin local testing
    addCommandAlias(
      "publishMillLocal",
      "set ThisBuild / version := \"0.0.0-TEST-SNAPSHOT\"; millPlugin/publishLocal"
    )
  )
  .aggregate(
    (
      api.projectRefs ++
        commandRunner.projectRefs ++
        core.projectRefs ++
        millPlugin.projectRefs ++
        sbtPlugin.projectRefs ++
        testRunner.projectRefs ++
        testkit.projectRefs ++
        testRunnerApi.projectRefs
    ) *
  )

lazy val core = (projectMatrix in file("modules") / "core")
  .settings(commonSettings, coreSettings, publishLocalDependsOn(api, testRunnerApi, testRunner))
  .dependsOn(api, testRunnerApi, testkit % Test)
  .jvmPlatform(scalaVersions = versions.crossScalaVersions)

lazy val commandRunner = (projectMatrix in file("modules") / "commandRunner")
  .defaultAxes(VirtualAxis.scalaABIVersion(versions.scala3), VirtualAxis.jvm)
  .settings(commonSettings, commandRunnerSettings, publishLocalDependsOn(core))
  .dependsOn(core, testkit % Test)
  .jvmPlatform(scalaVersions = Seq(versions.scala3))

// sbt plugins have to use Scala 2.12
lazy val sbtPlugin = (projectMatrix in file("modules") / "sbt")
  .enablePlugins(SbtPlugin)
  .defaultAxes(VirtualAxis.scalaPartialVersion("2.12"), VirtualAxis.jvm)
  .settings(commonSettings, sbtPluginSettings, publishLocalDependsOn(core, testRunner))
  .dependsOn(core, testkit % Test)
  .jvmPlatform(scalaVersions = Seq(versions.scala3, versions.scala212))

// Mill plugins are compiled with the Scala version of the minimum supported Mill version
lazy val millPlugin = (projectMatrix in file("modules") / "mill")
  .defaultAxes(VirtualAxis.scalaABIVersion(versions.scalaMill), VirtualAxis.jvm)
  .settings(
    commonSettings,
    millPluginSettings,
    publishLocalDependsOn(core, testRunner),
    millScripted := millScriptedTask
      .dependsOn(publishLocal, testRunner.jvm(versions.scala3Lts) / publishLocal)
      .value
  )
  .dependsOn(core, testkit % Test)
  .jvmPlatform(scalaVersions = Seq(versions.scalaMill))

lazy val testRunner = (projectMatrix in file("modules") / "testRunner")
  .settings(commonSettings, testRunnerSettings, publishLocalDependsOn(testRunnerApi))
  .dependsOn(testRunnerApi)
  .jvmPlatform(scalaVersions = versions.fullCrossScalaVersions)

lazy val testRunnerApi = (projectMatrix in file("modules") / "testRunnerApi")
  .settings(commonSettings, testRunnerApiSettings)
  .jvmPlatform(scalaVersions = versions.fullCrossScalaVersions)

lazy val api = (projectMatrix in file("modules") / "api")
  .settings(commonSettings, apiSettings)
  .jvmPlatform(scalaVersions = versions.fullCrossScalaVersions)

lazy val testkit = (projectMatrix in file("modules") / "testkit")
  .settings(commonSettings, testkitSettings, publishLocalDependsOn(api))
  .dependsOn(api)
  .jvmPlatform(scalaVersions = versions.crossScalaVersions)

def publishLocalDependsOn(matrixes: ProjectMatrix*) = {
  val projectRefs = matrixes.flatMap(_.projectRefs)
  Seq(
    publishLocal := publishLocal.dependsOn(projectRefs.map(_ / publishLocal) *).value,
    publishM2 := publishM2.dependsOn(projectRefs.map(_ / publishM2) *).value
  )
}
