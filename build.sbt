lazy val root = (project withId "stryker4s" in file("."))
  .settings(
    buildLevelSettings,
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

def buildLevelSettings: Seq[Setting[_]] = {
  inThisBuild(
    Seq(
      name := "stryker4s",
      version := "0.0.1",
      description := "Stryker4s the mutation testing framework for Scala.",
      organization := "io.stryker-mutator",
      organizationHomepage := Some(url("https://stryker-mutator.io/")),
      licenses += "Apache-2.0" -> url("https://github.com/stryker-mutator/stryker4s/blob/master/LICENSE"),
      developers := List(
        Developer("legopiraat", "Legopiraat", "", url("https://github.com/legopiraat")),
        Developer("hugo-vrijswijk", "Hugo", "@hugo_ijslijk", url("https://github.com/hugo-vrijswijk"))
      ),
      scmInfo := Some(ScmInfo(url("https://github.com/stryker-mutator/stryker4s"),
                              "git@github.com:stryker-mutator/stryker4s.git"))
    ))
}