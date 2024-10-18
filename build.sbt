import Dependencies.*
import Settings.*

lazy val root = (project withId "stryker4s" in file("."))
  .settings(
    buildLevelSettings,
    publish / skip := true,
    Global / onLoad ~= (_ andThen ("writeHooks" :: _)),
    crossScalaVersions := Nil,
    // Publish locally for sbt plugin testing
    addCommandAlias(
      "publishPluginLocal",
      "set ThisBuild / version := \"0.0.0-TEST-SNAPSHOT\"; ++ 2.12 sbtPlugin/publishLocal; + sbtTestRunner/publishLocal;"
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
  .aggregate(core, commandRunner, sbtPlugin, sbtTestRunner, testRunnerApi, api, testkit)

lazy val core = (project in file("modules") / "core")
  .settings(commonSettings, coreSettings, publishLocalDependsOn(api, testRunnerApi, testkit))
  .dependsOn(api, testRunnerApi, testkit % Test)

lazy val commandRunner = (project in file("modules") / "commandRunner")
  .settings(commonSettings, commandRunnerSettings, publishLocalDependsOn(core, testkit))
  .dependsOn(core, testkit % Test)

// sbt plugins have to use Scala 2.12
lazy val sbtPlugin = (project in file("modules") / "sbt")
  .enablePlugins(SbtPlugin)
  .settings(commonSettings, sbtPluginSettings, publishLocalDependsOn(core, sbtTestRunner))
  .dependsOn(core)

lazy val sbtTestRunner = (project in file("modules") / "sbtTestRunner")
  .settings(commonSettings, sbtTestRunnerSettings, publishLocalDependsOn(testRunnerApi))
  .dependsOn(testRunnerApi)

lazy val testRunnerApi = (project in file("modules") / "testRunnerApi")
  .settings(commonSettings, testRunnerApiSettings)

lazy val api = (project in file("modules") / "api")
  .settings(commonSettings, apiSettings)

lazy val testkit = (project in file("modules") / "testkit")
  .settings(commonSettings, testkitSettings, publishLocalDependsOn(api))
  .dependsOn(api)

lazy val writeHooks = taskKey[Unit]("Write git hooks")
Global / writeHooks := GitHooks(file("git-hooks"), file(".git/hooks"), streams.value.log)

def publishLocalDependsOn(matrixes: ProjectReference*) = {
  val projectRefs = matrixes
  Seq(
    publishLocal := publishLocal.dependsOn(projectRefs.map(_ / publishLocal) *).value,
    publishM2 := publishM2.dependsOn(projectRefs.map(_ / publishM2) *).value
  )
}
