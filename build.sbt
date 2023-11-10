import Dependencies.*
import Settings.*

lazy val root = (project withId "stryker4s" in file("."))
  .settings(
    buildLevelSettings,
    publish / skip := true,
    Global / onLoad ~= (_ andThen ("writeHooks" :: _)),
    // Publish locally for sbt plugin testing
    addCommandAlias(
      "publishPluginLocal",
      "set ThisBuild / version := \"0.0.0-TEST-SNAPSHOT\"; sbtPlugin/publishLocal; sbtTestRunner/publishLocal;"
    ),
    // Publish to .m2 folder for Maven plugin testing
    addCommandAlias(
      "publishM2Local",
      "set ThisBuild / version := \"SET-BY-SBT-SNAPSHOT\"; core/publishM2;"
    ),
    // Publish to .ivy folder for command runner local testing
    addCommandAlias(
      "publishCommandRunnerLocal",
      "set ThisBuild / version := \"0.0.0-TEST-SNAPSHOT\"; commandRunner/publishLocal"
    )
  )
  .aggregate(
    (core.projectRefs ++
      commandRunner.projectRefs ++
      sbtPlugin.projectRefs ++
      sbtTestRunner.projectRefs ++
      testRunnerApi.projectRefs ++
      api.projectRefs ++
      testkit.projectRefs)*
  )

lazy val core = (projectMatrix in file("modules") / "core")
  .settings(commonSettings, coreSettings)
  .dependsOn(api, testRunnerApi, testkit % Test)
  .aggregate(api, testRunnerApi, testkit)
  .jvmPlatform(scalaVersions = versions.crossScalaVersions)

lazy val commandRunner = (projectMatrix in file("modules") / "commandRunner")
  .settings(commonSettings, commandRunnerSettings)
  .dependsOn(core, testkit % Test)
  .aggregate(core, testkit)
  .jvmPlatform(scalaVersions = versions.crossScalaVersions)

// sbt plugins have to use Scala 2.12
lazy val sbtPlugin = (projectMatrix in file("modules") / "sbt")
  .enablePlugins(SbtPlugin)
  .defaultAxes(VirtualAxis.scalaPartialVersion("2.12"), VirtualAxis.jvm)
  .settings(commonSettings, sbtPluginSettings)
  .dependsOn(core)
  .aggregate(core)
  .jvmPlatform(scalaVersions = Seq(versions.scala212))

lazy val sbtTestRunner = (projectMatrix in file("modules") / "sbtTestRunner")
  .settings(commonSettings, sbtTestRunnerSettings)
  .dependsOn(testRunnerApi)
  .aggregate(testRunnerApi)
  .jvmPlatform(scalaVersions = versions.fullCrossScalaVersions)

lazy val testRunnerApi = (projectMatrix in file("modules") / "testRunnerApi")
  .settings(commonSettings, testRunnerApiSettings)
  .jvmPlatform(scalaVersions = versions.fullCrossScalaVersions)

// Pure Java module with interfaces
lazy val api = (projectMatrix in file("modules") / "api")
  .settings(apiSettings)
  .jvmPlatform(false)

lazy val testkit = (projectMatrix in file("modules") / "testkit")
  .settings(commonSettings, testkitSettings)
  .dependsOn(api)
  .aggregate(api)
  .jvmPlatform(scalaVersions = versions.crossScalaVersions)

lazy val writeHooks = taskKey[Unit]("Write git hooks")
Global / writeHooks := GitHooks(file("git-hooks"), file(".git/hooks"), streams.value.log)
