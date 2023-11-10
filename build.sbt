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
      "set ThisBuild / version := \"0.0.0-TEST-SNAPSHOT\"; core2_12/publishLocal; testRunnerApi2_12/publishLocal; sbtPlugin/publishLocal; testRunnerApi/publishLocal; sbtTestRunner/publishLocal"
    ),
    // Publish to .m2 folder for Maven plugin testing
    addCommandAlias(
      "publishM2Local",
      "set ThisBuild / version := \"SET-BY-SBT-SNAPSHOT\"; core/publishM2; testRunnerApi/publishM2"
    ),
    // Publish to .ivy folder for command runner local testing
    addCommandAlias(
      "publishCommandRunnerLocal",
      "set ThisBuild / version := \"0.0.0-TEST-SNAPSHOT\"; core/publishLocal; testRunnerApi/publishLocal; commandRunner/publishLocal"
    )
  )
  .aggregate(
    (core.projectRefs ++
      commandRunner.projectRefs ++
      sbtPlugin.projectRefs ++
      testRunnerApi.projectRefs ++
      sbtTestRunner.projectRefs)*
  )

lazy val core = (projectMatrix in file("modules") / "core")
  .settings(name := "stryker4s-core", commonSettings, coreSettings)
  .dependsOn(api, testRunnerApi)
  .jvmPlatform(scalaVersions = versions.crossScalaVersions)

lazy val commandRunner = (projectMatrix in file("modules") / "commandRunner")
  .settings(name := "stryker4s-command-runner", commonSettings, commandRunnerSettings)
  .dependsOn(core, core % "test->test")
  .jvmPlatform(scalaVersions = versions.crossScalaVersions)

// sbt plugins have to use Scala 2.12
lazy val sbtPlugin = (projectMatrix in file("modules") / "sbt")
  .enablePlugins(SbtPlugin)
  .defaultAxes(VirtualAxis.scalaPartialVersion("2.12"), VirtualAxis.jvm)
  .settings(name := "sbt-stryker4s", commonSettings, sbtPluginSettings)
  .dependsOn(core)
  .jvmPlatform(scalaVersions = Seq(versions.scala212))

lazy val sbtTestRunner = (projectMatrix in file("modules") / "sbtTestRunner")
  .settings(name := "stryker4s-sbt-testrunner", commonSettings, sbtTestRunnerSettings)
  .dependsOn(testRunnerApi)
  .jvmPlatform(scalaVersions = versions.fullCrossScalaVersions)

lazy val testRunnerApi = (projectMatrix in file("modules") / "testRunnerApi")
  .settings(name := "stryker4s-testrunner-api", commonSettings, testRunnerApiSettings)
  .jvmPlatform(scalaVersions = versions.fullCrossScalaVersions)

// Pure Java module with interfaces
lazy val api = (projectMatrix in file("modules") / "api")
  .settings(name := "stryker4s-api", apiSettings)
  .jvmPlatform(false)

lazy val writeHooks = taskKey[Unit]("Write git hooks")
Global / writeHooks := GitHooks(file("git-hooks"), file(".git/hooks"), streams.value.log)
