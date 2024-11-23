import sbt.*

object Dependencies {
  object versions {
    val scala212 = "2.12.20"

    val scala213 = "2.13.15"

    val scala3 = "3.3.4"

    val fullCrossScalaVersions = Seq(scala3, scala213, scala212)

    // Test dependencies
    val munit = "1.0.2"

    val munitCatsEffect = "2.0.0"

    // Direct dependencies
    val catsCore = "2.12.0"

    val catsEffect = "3.5.6"

    val circe = "0.14.10"

    val ciris = "3.6.0"

    val fansi = "0.5.0"

    val hocon = "1.4.3"

    val fs2 = "3.11.0"

    val mutationTestingElements = "3.4.0"

    val mutationTestingMetrics = "3.4.0"

    val scalameta = "4.11.1"

    val slf4j = "2.0.16"

    val sttp = "3.10.1"

    val testInterface = "1.0"

    val weaponRegeX = "1.3.2"
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
  val mutationTestingElements = "io.stryker-mutator" % "mutation-testing-elements" % versions.mutationTestingElements
  val mutationTestingMetrics =
    "io.stryker-mutator" %% "mutation-testing-metrics-circe" % versions.mutationTestingMetrics
  // Exclude some 2.13 dependencies when on scala 3 to avoid conflicts
  val scalameta = ("org.scalameta" %% "scalameta" % versions.scalameta)
    .cross(CrossVersion.for3Use2_13)
    .exclude("com.lihaoyi", "sourcecode_2.13")
    .exclude("com.thesamet.scalapb", "scalapb-runtime_2.13")
  val scalapbRuntime =
    "com.thesamet.scalapb" %% "scalapb-runtime" % scalapb.compiler.Version.scalapbVersion % "protobuf"
  val slf4j = "org.slf4j" % "slf4j-simple" % versions.slf4j
  val sttpCirce = "com.softwaremill.sttp.client3" %% "circe" % versions.sttp
  val sttpFs2Backend = "com.softwaremill.sttp.client3" %% "fs2" % versions.sttp
  val testInterface = "org.scala-sbt" % "test-interface" % versions.testInterface
  val weaponRegeX = "io.stryker-mutator" %% "weapon-regex" % versions.weaponRegeX

}
