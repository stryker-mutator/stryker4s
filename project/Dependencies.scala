import sbt._

object Dependencies {
  object versions {
    val scala211 = "2.11.12"
    val scala212 = "2.12.11"
    val scala213 = "2.13.3"
    val dotty = "0.25.0-RC2"

    /** Cross-versions for main projects
      */
    val crossScalaVersions = Seq(scala213, scala212)

    /** Fuller cross-versions (used for injected packages)
      */
    val fullCrossScalaVersions = crossScalaVersions ++ Seq(scala211, dotty)

    val testInterface = "1.0"
    val scalameta = "4.3.20"
    val pureconfig = "0.13.0"
    val scalatest = "3.2.0"
    val mockitoScala = "1.14.8"
    val betterFiles = "3.9.1"
    val log4j = "2.13.3"
    val grizzledSlf4j = "1.3.4"
    val cats = "2.0.0"
    val circe = "0.13.0"
    val mutationTestingElements = "1.3.1"
    val mutationTestingMetrics = "1.3.1"
    val sttp = "2.2.1"
    val fs2 = "2.4.2"
  }

  object test {
    val scalatest = "org.scalatest" %% "scalatest" % versions.scalatest % Test
    val mockitoScala = "org.mockito" %% "mockito-scala-scalatest" % versions.mockitoScala % Test
  }

  val testInterface = "org.scala-sbt" % "test-interface" % versions.testInterface
  val pureconfig = "com.github.pureconfig" %% "pureconfig" % versions.pureconfig
  val scalameta = "org.scalameta" %% "scalameta" % versions.scalameta
  val betterFiles = "com.github.pathikrit" %% "better-files" % versions.betterFiles
  val log4jApi = "org.apache.logging.log4j" % "log4j-api" % versions.log4j
  val log4jCore = "org.apache.logging.log4j" % "log4j-core" % versions.log4j
  val log4jslf4jImpl = "org.apache.logging.log4j" % "log4j-slf4j-impl" % versions.log4j
  val grizzledSlf4j = "org.clapper" %% "grizzled-slf4j" % versions.grizzledSlf4j
  val catsCore = "org.typelevel" %% "cats-core" % versions.cats
  val circeCore = "io.circe" %% "circe-core" % versions.circe
  val sttpCirce = "com.softwaremill.sttp.client" %% "circe" % versions.sttp
  val sttpCatsBackend = "com.softwaremill.sttp.client" %% "async-http-client-backend-cats" % versions.sttp
  val mutationTestingElements = "io.stryker-mutator" % "mutation-testing-elements" % versions.mutationTestingElements
  val mutationTestingMetrics =
    "io.stryker-mutator" %% "mutation-testing-metrics-circe" % versions.mutationTestingMetrics
  val fs2Core = "co.fs2" %% "fs2-core" % versions.fs2
  val fs2IO = "co.fs2" %% "fs2-io" % versions.fs2
}
