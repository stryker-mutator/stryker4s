lazy val root = (project withId "stryker4s" in file("."))
  .settings(
    name := "stryker4s",
    crossScalaVersions := Dependencies.versions.crossScala,
  )
  .aggregate(stryker4sCore, stryker4sUtil)
  .dependsOn(stryker4sCore) // So `sbt run` can be used in root project

lazy val stryker4sCore = (project withId "stryker4s-core" in file("core"))
  .settings(
    mainClass := Some("stryker4s.run.Stryker4sRunner")
  )
  .settings(Settings.commonSettings)
  .dependsOn(stryker4sUtil)
  .dependsOn(stryker4sUtil % "test -> test")

lazy val stryker4sUtil = (project withId "stryker4s-util" in file("util"))
  .settings(Settings.commonSettings)
