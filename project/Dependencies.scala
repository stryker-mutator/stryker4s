import sbt._

object Dependencies {
  object versions {
    val scala211 = "2.11.12"
    val scala212 = "2.12.13"
    val scala213 = "2.13.5"
    val scala3 = "3.0.0-RC3"

    /** Cross-versions for main projects
      */
    val crossScalaVersions = Seq(scala213, scala212)

    /** Fuller cross-versions (used for injected packages like stryker4s-api and sbt-stryker4s-testrunner)
      */
    val fullCrossScalaVersions = crossScalaVersions ++ Seq(scala211, scala3)

    // Test dependencies
    val catsEffectScalaTest = "1.0.1"
    val mockitoScala = "1.16.37"
    val scalatest = "3.2.7"

    // Direct dependencies
    val betterFiles = "3.9.1"
    val catsCore = "2.5.0"
    val catsEffect = "3.0.2"
    val circe = "0.13.0"
    val fs2 = "3.0.1"
    val log4j = "2.14.1"
    val mutationTestingElements = "1.7.0"
    val mutationTestingMetrics = "1.7.0"
    val pureconfig = "0.14.1"
    val scalameta = "4.4.13"
    val sttp = "3.3.0-RC2"
    val testInterface = "1.0"
    val weaponRegeX = "0.4.1"
  }

  object test {
    val catsEffectScalaTest = "org.typelevel" %% "cats-effect-testing-scalatest" % versions.catsEffectScalaTest % Test
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
  val testInterface = "org.scala-sbt" % "test-interface" % versions.testInterface
  val weaponRegeX = "io.stryker-mutator" %% "weapon-regex" % versions.weaponRegeX

}
