import Dependencies._
import Settings._

lazy val root = (project withId "stryker4s" in file("."))
  .settings(
    buildLevelSettings,
    skip in publish := true,
    onLoad in Global ~= (_ andThen ("writeHooks" :: _))
  )
  .aggregate(
    (stryker4sCore.projectRefs ++
      stryker4sCommandRunner.projectRefs ++
      sbtStryker4s.projectRefs ++
      api.projectRefs ++
      sbtTestRunner.projectRefs): _*
  )

lazy val stryker4sCore = newProject("stryker4s-core", "core")
  .settings(coreSettings)
  .dependsOn(api)
  .jvmPlatform(scalaVersions = versions.crossScalaVersions)

lazy val stryker4sCommandRunner = newProject("stryker4s-command-runner", "command-runner")
  .settings(
    commandRunnerSettings,
    mainClass in (Compile, run) := Some("stryker4s.run.Stryker4sCommandRunner")
  )
  .dependsOn(stryker4sCore, stryker4sCore % "test->test")
  .jvmPlatform(scalaVersions = versions.crossScalaVersions)

// sbt plugins have to use Scala 2.12
lazy val sbtStryker4s = newProject("sbt-stryker4s", "sbt")
  .enablePlugins(SbtPlugin)
  .settings(commonSettings, sbtPluginSettings)
  .dependsOn(stryker4sCore)
  .jvmPlatform(scalaVersions = Seq(versions.scala212))

lazy val sbtTestRunner = newProject("sbt-stryker4s-testrunner", "sbt-testrunner")
  .settings(sbtTestrunnerSettings)
  .dependsOn(api, api % "test->test")
  .jvmPlatform(scalaVersions = versions.fullCrossScalaVersions)

lazy val api = newProject("stryker4s-api", "api")
  .settings(apiSettings)
  .jvmPlatform(scalaVersions = versions.fullCrossScalaVersions)

def newProject(projectName: String, dir: String) =
  sbt.internal
    .ProjectMatrix(projectName, file(dir))
    .settings(commonSettings)

lazy val writeHooks = taskKey[Unit]("Write git hooks")
Global / writeHooks := GitHooks(file("git-hooks"), file(".git/hooks"), streams.value.log)
