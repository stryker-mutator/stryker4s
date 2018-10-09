import xerial.sbt.Sonatype.SonatypeKeys.sonatypePublishTo

lazy val root = (project withId "stryker4s" in file("."))
  .settings(
    Settings.buildLevelSettings,
    publishTo := sonatypePublishTo.value,
    mainClass in (Compile, run) := Some("stryker4s.run.Stryker4sRunner")
  )
  .aggregate(stryker4sCore, stryker4sUtil, sbtStryker4s)
  .dependsOn(stryker4sCore)

lazy val stryker4sCore = (project withId "stryker4s-core" in file("core"))
  .settings(Settings.commonSettings)
  .dependsOn(stryker4sUtil)
  .dependsOn(stryker4sUtil % "test -> test")

lazy val stryker4sUtil = (project withId "stryker4s-util" in file("util"))
  .settings(Settings.commonSettings)

lazy val sbtStryker4s = (project withId "sbt-stryker4s" in file("sbt"))
  .enablePlugins(SbtPlugin)
  .settings(
    Settings.buildLevelSettings,
    version := "0.1.0-SNAPSHOT",
    scriptedLaunchOpts := {
      scriptedLaunchOpts.value ++
        Seq("-Xmx1024M", "-Dplugin.version=" + version.value)
    },
    scriptedBufferLog := false
  )
  .dependsOn(stryker4sCore)
