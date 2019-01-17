lazy val root = (project withId "stryker4s" in file("."))
  .settings(
    Settings.buildLevelSettings,
    skip in publish := true,
    mainClass in (Compile, run) := Some("stryker4s.run.Stryker4sRunner")
  )
  .aggregate(stryker4sCore, stryker4sUtil, sbtStryker4s)
  .dependsOn(stryker4sCore)

lazy val stryker4sCore = (project withId "stryker4s-core" in file("core"))
  .settings(Settings.commonSettings)
  .settings(Settings.coreSettings)
  .dependsOn(stryker4sUtil)
  .dependsOn(stryker4sUtil % "test -> test")

lazy val stryker4sUtil = (project withId "stryker4s-util" in file("util"))
  .settings(Settings.commonSettings) //TODO: Remove me when completed

lazy val stryker4sReporter = (project withId "stryker4s-reporter" in file("reporter"))
  .settings(Settings.commonSettings)

lazy val stryker4sIntegrationTests = (project withId "stryker4s-integration-tests" in file("integration-tests"))
  .settings(Settings.commonSettings)

/**
  * Runners
  */
lazy val stryker4sCommandRunner = (project withId "stryker4s-command-runner" in file("runners/command-runner"))
  .settings(Settings.commonSettings)

lazy val sbtStryker4s = (project withId "sbt-stryker4s" in file("runners/sbt"))
  .enablePlugins(SbtPlugin)
  .settings(
    Settings.commonSettings,
    scriptedLaunchOpts := {
      scriptedLaunchOpts.value ++
        Seq("-Xmx1024M", "-Dplugin.version=" + version.value)
    },
    scriptedBufferLog := false
  )
  .dependsOn(stryker4sCore)
