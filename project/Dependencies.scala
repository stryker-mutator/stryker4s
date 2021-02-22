import sbt._

object Dependencies {
  object versions {
    val scala211 = "2.11.12"
    val scala212 = "2.12.13"
    val scala213 = "2.13.4"
    val scala3 = "3.0.0-RC1"

    /** Cross-versions for main projects
      */
    val crossScalaVersions = Seq(scala213, scala212)

    /** Fuller cross-versions (used for injected packages like stryker4s-api and sbt-stryker4s-testrunner)
      */
    val fullCrossScalaVersions = crossScalaVersions ++ Seq(scala211, scala3)

    // Test dependencies
    val catsEffectScalaTest = "0.5.1"
    val mockitoScala = "1.16.25"
    val scalatest = "3.2.5"

    // Direct dependencies
    val betterFiles = "3.9.1"
    val catsCore = "2.4.2"
    val catsEffect = "2.3.3"
    val circe = "0.13.0"
    val fs2 = "2.5.3"
    val log4j = "2.14.0"
    val mutationTestingElements = "1.6.0"
    val mutationTestingMetrics = "1.6.0"
    val pureconfig = "0.14.0"
    val scalameta = "4.4.9"
    val sttp = "3.1.3"
    val sttpModel = "1.3.3"
    val testInterface = "1.0"
    val weaponRegeX = "0.3.0"
  }

  object test {
    val catsEffectScalaTest = "com.codecommit" %% "cats-effect-testing-scalatest" % versions.catsEffectScalaTest % Test
    val mockitoScala = "org.mockito" %% "mockito-scala-scalatest" % versions.mockitoScala % Test
    val mockitoScalaCats = "org.mockito" %% "mockito-scala-cats" % versions.mockitoScala % Test
    val scalatest = "org.scalatest" %% "scalatest" % versions.scalatest % Test
  }

  val betterFiles = "com.github.pathikrit" %% "better-files" % versions.betterFiles
  val catsCore = "org.typelevel" %% "cats-core" % versions.catsCore
  val catsEffect = "org.typelevel" %% "cats-effect" % versions.catsEffect
  val circeCore = "io.circe" %% "circe-core" % versions.circe
  val fs2Core = "co.fs2" %% "fs2-core" % versions.fs2
  val fs2IO = "co.fs2" %% "fs2-io" % versions.fs2
  val log4j = "org.apache.logging.log4j" % "log4j-slf4j-impl" % versions.log4j
  val mutationTestingElements = "io.stryker-mutator" % "mutation-testing-elements" % versions.mutationTestingElements
  val mutationTestingMetrics =
    "io.stryker-mutator" %% "mutation-testing-metrics-circe" % versions.mutationTestingMetrics
  val pureconfig = "com.github.pureconfig" %% "pureconfig" % versions.pureconfig
  val pureconfigSttp = "com.github.pureconfig" %% "pureconfig-sttp" % versions.pureconfig
  val scalameta = "org.scalameta" %% "scalameta" % versions.scalameta
  val sttpCirce = "com.softwaremill.sttp.client3" %% "circe" % versions.sttp
  val sttpFs2Backend = "com.softwaremill.sttp.client3" %% "httpclient-backend-fs2" % versions.sttp
  // To prevent dependency resolvement getting a lower version, directly depend on the latest version of sttp-model
  val sttpModel = "com.softwaremill.sttp.model" %% "core" % versions.sttpModel
  val testInterface = "org.scala-sbt" % "test-interface" % versions.testInterface
  val weaponRegeX = "io.stryker-mutator" %% "weapon-regex" % versions.weaponRegeX

}
