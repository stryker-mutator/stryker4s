lazy val root = (project withId "stryker4s" in file("."))
  .settings(
    name := "stryker4s",
    crossScalaVersions := Dependencies.versions.crossScala,
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
  .settings(
    version := "0.0.1-SNAPSHOT",
    name := "sbt-stryker4s",
    organization := "io.stryker-mutator",
    sbtPlugin := true,
    scriptedLaunchOpts := { scriptedLaunchOpts.value ++
      Seq("-Xmx1024M", "-Dplugin.version=" + version.value)
    },
    scriptedBufferLog := false
  )
  .dependsOn(stryker4sCore)
