import xerial.sbt.Sonatype.SonatypeKeys.sonatypePublishTo

lazy val root = (project withId "stryker4s" in file("."))
  .settings(
    Settings.buildLevelSettings,
    publishTo := sonatypePublishTo.value,
    crossScalaVersions := Dependencies.versions.crossScala,
    mainClass in (Compile, run) := Some("stryker4s.run.Stryker4sRunner")
  )
  .aggregate(stryker4sCore, stryker4sUtil)
  .dependsOn(stryker4sCore)

lazy val stryker4sCore = (project withId "stryker4s-core" in file("core"))
  .settings(Settings.commonSettings)
  .dependsOn(stryker4sUtil)
  .dependsOn(stryker4sUtil % "test -> test")

lazy val stryker4sUtil = (project withId "stryker4s-util" in file("util"))
  .settings(Settings.commonSettings)