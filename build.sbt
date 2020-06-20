import Dependencies._
import Settings._

lazy val root = (project withId "stryker4s" in file("."))
  .settings(
    buildLevelSettings,
    skip in publish := true,
    onLoad in Global ~= (_ andThen ("writeHooks" :: _))
  )
  .aggregate(
    stryker4sCore.jvm(versions.scala213),
    stryker4sCommandRunner.jvm(versions.scala213),
    sbtStryker4s
  )

lazy val stryker4sCore = newProject("stryker4s-core", "core")
  .settings(coreSettings)
  .jvmPlatform(scalaVersions = versions.crossScalaVersions)

lazy val stryker4sCommandRunner = newProject("stryker4s-command-runner", "command-runner")
  .settings(
    commandRunnerSettings,
    mainClass in (Compile, run) := Some("stryker4s.run.Stryker4sCommandRunner")
  )
  .dependsOn(stryker4sCore, stryker4sCore % "test->test")
  .jvmPlatform(scalaVersions = versions.crossScalaVersions)

// sbt project is a 'normal' project without projectMatrix because there is only 1 scala version
// sbt plugins have to use Scala 2.12
lazy val sbtStryker4s = (project withId "sbt-stryker4s" in file("sbt"))
  .enablePlugins(SbtPlugin)
  .settings(commonSettings, sbtPluginSettings)
  .dependsOn(stryker4sCore.jvm(versions.scala212), api.jvm(versions.scala212))

lazy val sbtTestRunner = newProject("sbt-stryker4s-testrunner", "sbt-testrunner")
  .settings(sbtTestrunnerSettings)
  .dependsOn(api)
  .jvmPlatform(scalaVersions = versions.crossScalaVersions)

lazy val api = newProject("stryker4s-api", "api")
  .settings(apiSettings)
  .dependsOn(stryker4sCore % "test->test")
  .jvmPlatform(scalaVersions = versions.crossScalaVersions)

def newProject(projectName: String, dir: String) =
  sbt.internal
    .ProjectMatrix(projectName, file(dir))
    .settings(commonSettings)

lazy val writeHooks = taskKey[Unit]("Write git hooks")
Global / writeHooks := GitHooks(file("git-hooks"), file(".git/hooks"), streams.value.log)
