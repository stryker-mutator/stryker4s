import Dependencies.*
import Settings.*
import sbt.internal.ProjectMatrix

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
      testkit.projectRefs) *
  )

lazy val core = (projectMatrix in file("modules") / "core")
  .settings(commonSettings, coreSettings, publishLocalDependsOn(api, testRunnerApi, testkit))
  .dependsOn(api, testRunnerApi, testkit % Test)
  .jvmPlatform(scalaVersions = versions.fullCrossScalaVersions)

lazy val commandRunner = (projectMatrix in file("modules") / "commandRunner")
  .settings(commonSettings, commandRunnerSettings, publishLocalDependsOn(core, testkit))
  .dependsOn(core, testkit % Test)
  .jvmPlatform(scalaVersions = versions.fullCrossScalaVersions)

// sbt plugins have to use Scala 2.12
lazy val sbtPlugin = (projectMatrix in file("modules") / "sbt")
  .enablePlugins(SbtPlugin)
  .defaultAxes(VirtualAxis.scalaPartialVersion("2.12"), VirtualAxis.jvm)
  .settings(commonSettings, sbtPluginSettings, publishLocalDependsOn(core))
  .dependsOn(core)
  .jvmPlatform(scalaVersions = Seq(versions.scala212 /* , versions.scala3 */ ))

lazy val sbtTestRunner = (projectMatrix in file("modules") / "sbtTestRunner")
  .settings(commonSettings, sbtTestRunnerSettings, publishLocalDependsOn(testRunnerApi))
  .dependsOn(testRunnerApi)
  .jvmPlatform(scalaVersions = versions.fullCrossScalaVersions)

lazy val testRunnerApi = (projectMatrix in file("modules") / "testRunnerApi")
  .settings(commonSettings, testRunnerApiSettings)
  .jvmPlatform(scalaVersions = versions.fullCrossScalaVersions)

lazy val api = (projectMatrix in file("modules") / "api")
  .settings(commonSettings, apiSettings)
  .jvmPlatform(scalaVersions = versions.fullCrossScalaVersions)

lazy val testkit = (projectMatrix in file("modules") / "testkit")
  .settings(commonSettings, testkitSettings, publishLocalDependsOn(api))
  .dependsOn(api)
  .jvmPlatform(scalaVersions = versions.fullCrossScalaVersions)

lazy val writeHooks = taskKey[Unit]("Write git hooks")
Global / writeHooks := GitHooks(file("git-hooks"), file(".git/hooks"), streams.value.log)

def publishLocalDependsOn(matrixes: ProjectMatrix*) = {
  val projectRefs = matrixes.flatMap(_.projectRefs)
  Seq(
    publishLocal := publishLocal.dependsOn(projectRefs.map(_ / publishLocal) *).value,
    publishM2 := publishM2.dependsOn(projectRefs.map(_ / publishM2) *).value
  )
}
