import Dependencies.*
import Settings.*

lazy val root = (project withId "stryker4s" in file("."))
  .settings(
    buildLevelSettings,
    publish / skip := true,
    Global / onLoad ~= (_ andThen ("writeHooks" :: _)),
    // Publish locally for sbt plugin testing
    addCommandAlias(
      "publishPluginLocal",
      "set ThisBuild / version := \"0.0.0-TEST-SNAPSHOT\"; stryker4s-core2_12/publishLocal; stryker4s-api2_12/publishLocal; sbt-stryker4s2_12/publishLocal; stryker4s-api/publishLocal; sbt-stryker4s-testrunner/publishLocal"
    ),
    // Publish to .m2 folder for Maven plugin testing
    addCommandAlias(
      "publishM2Local",
      "set ThisBuild / version := \"SET-BY-SBT-SNAPSHOT\"; stryker4s-core/publishM2; stryker4s-api/publishM2"
    ),
    // Publish to .ivy folder for command runner local testing
    addCommandAlias(
      "publishCommandRunnerLocal",
      "set ThisBuild / version := \"0.0.0-TEST-SNAPSHOT\"; stryker4s-core/publishLocal; stryker4s-api/publishLocal; stryker4s-command-runner/publishLocal"
    )
  )
  .aggregate(
    (stryker4sCore.projectRefs ++
      stryker4sCommandRunner.projectRefs ++
      sbtStryker4s.projectRefs ++
      stryker4sApi.projectRefs ++
      sbtTestRunner.projectRefs) *
  )

lazy val stryker4sCore = newProject("stryker4s-core", "core")
  .settings(coreSettings)
  .dependsOn(stryker4sApi)
  .jvmPlatform(scalaVersions = versions.crossScalaVersions)

lazy val stryker4sCommandRunner = newProject("stryker4s-command-runner", "command-runner")
  .settings(
    commandRunnerSettings
  )
  .dependsOn(stryker4sCore, stryker4sCore % "test->test")
  .jvmPlatform(scalaVersions = versions.crossScalaVersions)

// sbt plugins have to use Scala 2.12
lazy val sbtStryker4s = newProject("sbt-stryker4s", "sbt")
  .enablePlugins(SbtPlugin)
  .settings(sbtPluginSettings)
  .dependsOn(stryker4sCore)
  .jvmPlatform(scalaVersions = Seq(versions.scala212))

lazy val sbtTestRunner = newProject("sbt-stryker4s-testrunner", "sbt-testrunner")
  .settings(sbtTestrunnerSettings)
  .dependsOn(stryker4sApi)
  .jvmPlatform(scalaVersions = versions.fullCrossScalaVersions)

lazy val stryker4sApi = newProject("stryker4s-api", "api")
  .settings(apiSettings)
  .jvmPlatform(scalaVersions = versions.fullCrossScalaVersions)

def newProject(projectName: String, dir: String) =
  sbt.internal
    .ProjectMatrix(projectName, file(dir))
    .settings(commonSettings)

lazy val writeHooks = taskKey[Unit]("Write git hooks")
Global / writeHooks := GitHooks(file("git-hooks"), file(".git/hooks"), streams.value.log)

lazy val jvmRoot = (project withId "stryker4jvm" in file("."))
  .settings(
    buildLevelSettings,
    publish / skip := true,
    Global / onLoad ~= (_ andThen ("writeHooks" :: _))
  )
  .aggregate(
    (stryker4jvmCore.projectRefs ++
      stryker4jvm.projectRefs ++
      stryker4jvmMutatorKotlin.projectRefs ++
      stryker4jvmMutatorScala.projectRefs ++
      stryker4sCore.projectRefs ++
      stryker4sCommandRunner.projectRefs ++
      sbtStryker4s.projectRefs ++
      stryker4sApi.projectRefs ++
      sbtTestRunner.projectRefs) *
  )

lazy val stryker4jvmCore = newProject("stryker4jvm-core", "stryker4jvm-core")
  .settings(jvmCoreSettings)
  .dependsOn(stryker4sApi)
  .jvmPlatform(scalaVersions = versions.crossScalaVersions)

lazy val stryker4jvm = newProject("stryker4jvm", "stryker4jvm")
  .dependsOn(stryker4jvmCore)
  .dependsOn(stryker4jvmMutatorKotlin)
  .dependsOn(stryker4jvmMutatorScala)
  .jvmPlatform(scalaVersions = versions.crossScalaVersions)

lazy val stryker4jvmMutatorKotlin = newProject("stryker4jvm-mutator-kotlin", "stryker4jvm-mutator-kotlin")
  .settings(
    Compile / kotlinSource := baseDirectory.value / "src/main/kotlin",
    crossScalaVersions := List(versions.scala212),
    crossPaths := false,
    autoScalaLibrary := false,

    // Include Kotlin files in sources
    Compile / packageConfiguration := {
      val old = (Compile / packageSrc / packageConfiguration).value
      val newSources = (Compile / sourceDirectories).value.flatMap(_ ** "*.kt" get)

      new Package.Configuration(
        old.sources ++ newSources.map(f => f -> f.getName),
        old.jar,
        old.options
      )
    },
    libraryDependencies ++= Seq(
      "org.jetbrains.kotlin" % "kotlin-stdlib-jdk8" % "1.5.10",
      "org.jetbrains.kotlin" % "kotlin-stdlib-common" % "1.5.10",
      "org.jetbrains.kotlin" % "kotlin-compiler-embeddable" % "1.5.10"
    ),
    libraryDependencies ++= Seq(
      "org.junit.jupiter" % "junit-jupiter-api" % "5.7.0"
    ).map(_ % Test)
  )
  .dependsOn(stryker4jvmCore)
  .jvmPlatform(scalaVersions = versions.crossScalaVersions)

lazy val stryker4jvmMutatorScala = newProject("stryker4jvm-mutator-scala", "stryker4jvm-mutator-scala")
  .settings(jvmMutatorScalaSettings)
  .dependsOn(stryker4jvmCore)
  .jvmPlatform(scalaVersions = versions.crossScalaVersions)
