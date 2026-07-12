import sbt.*

object Dependencies {
  object versions {
    val scala212 = "2.12.21"

    val scala213 = "2.13.18"

    // sbt-test-runner uses LTS to ensure compatibility with any project it runs in
    val scala3Lts = "3.3.8"

    val scala3 = "3.8.4"

    // Mill plugins must be compiled with the same Scala version as the minimum supported Mill version
    val scalaMill = "3.8.2"

    // All supported Scala versions
    val fullCrossScalaVersions = Seq(scala3Lts, scala213, scala212)

    // Scala 3 versions and 2.12 (for sbtPlugin)
    val crossScalaVersions = Seq(scala3Lts, scala3, scalaMill, scala212).distinct

    // Test dependencies
    val munit = "1.3.4"

    val munitCatsEffect = "2.2.0"

    // Direct dependencies
    val catsCore = "2.13.0"

    val catsEffect = "3.7.0"

    val circe = "0.14.16"

    val ciris = "3.15.0"

    val fansi = "0.5.1"

    val fs2 = "3.13.0"

    val hocon = "1.4.9"

    val mill = "1.1.7"

    val mutationTestingElements = "3.8.4"

    val mutationTestingMetrics = "3.8.4"

    val scalameta = "4.17.2"

    val scopt = "4.1.0"

    val slf4j = "2.0.18"

    val sttp = "4.0.26"

    val testInterface = "1.0"

    val weaponRegeX = "2.0.4"

  }

  object test {
    val munit = "org.scalameta" %% "munit" % versions.munit
    val munitCatsEffect = "org.typelevel" %% "munit-cats-effect" % versions.munitCatsEffect
  }

  val catsCore = "org.typelevel" %% "cats-core" % versions.catsCore
  val catsEffect = "org.typelevel" %% "cats-effect" % versions.catsEffect
  val circeCore = "io.circe" %% "circe-core" % versions.circe
  val ciris = "is.cir" %% "ciris" % versions.ciris
  val fansi = "com.lihaoyi" %% "fansi" % versions.fansi
  val fs2Core = "co.fs2" %% "fs2-core" % versions.fs2
  val fs2IO = "co.fs2" %% "fs2-io" % versions.fs2
  val hocon = "com.typesafe" % "config" % versions.hocon
  val millLibsScalalib = "com.lihaoyi" %% "mill-libs-scalalib" % versions.mill
  val millTestkit = "com.lihaoyi" %% "mill-testkit" % versions.mill
  val mutationTestingElements = "io.stryker-mutator" % "mutation-testing-elements" % versions.mutationTestingElements
  val mutationTestingMetrics = Seq(
    "io.stryker-mutator" %% "mutation-testing-metrics" % versions.mutationTestingMetrics,
    "io.stryker-mutator" %% "mutation-testing-metrics-circe" % versions.mutationTestingMetrics,
    "io.stryker-mutator" %% "mutation-testing-metrics-cats" % versions.mutationTestingMetrics
  )
  val scalameta = "org.scalameta" %% "scalameta" % versions.scalameta
  val scalapbRuntime =
    "com.thesamet.scalapb" %% "scalapb-runtime" % scalapb.compiler.Version.scalapbVersion % "protobuf"
  val scopt = "com.github.scopt" %% "scopt" % versions.scopt
  val slf4j = "org.slf4j" % "slf4j-simple" % versions.slf4j
  val sttpCirce = "com.softwaremill.sttp.client4" %% "circe" % versions.sttp
  val sttpFs2Backend = "com.softwaremill.sttp.client4" %% "fs2" % versions.sttp
  val testInterface = "org.scala-sbt" % "test-interface" % versions.testInterface
  val weaponRegeX = "io.stryker-mutator" %% "weapon-regex" % versions.weaponRegeX

}
