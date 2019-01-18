lazy val root = (project withId "stryker4s" in file("."))
  .settings(
    Settings.buildLevelSettings,
    skip in publish := true,
    mainClass in (Compile, run) := Some("stryker4s.run.Stryker4sCommandRunner")
  )
  .aggregate(stryker4sCore, sbtStryker4s)
  .dependsOn(stryker4sCommandRunner) // So 'run' command also works from root of project

lazy val stryker4sCore = (project withId "stryker4s-core" in file("core"))
  .settings(Settings.commonSettings)

lazy val stryker4sReporter = (project withId "stryker4s-reporter" in file("reporter"))
  .settings(Settings.commonSettings)

/**
  * Runners
  */
lazy val stryker4sCommandRunner = (project withId "stryker4s-command-runner" in file("runners/command-runner"))
  .settings(Settings.commonSettings, Settings.commandRunnerSettings)
  .dependsOn(stryker4sCore)

lazy val sbtStryker4s = (project withId "sbt-stryker4s" in file("runners/sbt"))
  .enablePlugins(SbtPlugin)
  .settings(Settings.commonSettings, Settings.sbtPluginSettings)
  .dependsOn(stryker4sCore)
