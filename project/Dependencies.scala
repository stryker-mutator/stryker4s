import sbt.*

object Dependencies {
  object versions {
    val scala212 = "2.12.19"

    val scala213 = "2.13.14"

    val scala3 = "3.3.3"

    /** Cross-versions for main projects
      */
    val crossScalaVersions = Seq(scala213, scala212)

    /** Fuller cross-versions (used for injected packages like testRunnerApi and sbtTestRunner)
      */
    val fullCrossScalaVersions = crossScalaVersions ++ Seq(scala3)

    // Test dependencies
    val munit = "1.0.0"

    val munitCatsEffect = "2.0.0"

    // Direct dependencies
    val catsCore = "2.10.0"

    val catsEffect = "3.5.4"

    val circe = "0.14.7"

    val fansi = "0.5.0"

    val fs2 = "3.10.2"

    val mutationTestingElements = "3.1.0"

    val mutationTestingMetrics = "3.1.0"

    val pureconfig = "0.17.6"

    val scalameta = "4.9.5"

    val slf4j = "2.0.13"

    val sttp = "3.9.7"

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
  val fansi = "com.lihaoyi" %% "fansi" % versions.fansi
  val fs2Core = "co.fs2" %% "fs2-core" % versions.fs2
  val fs2IO = "co.fs2" %% "fs2-io" % versions.fs2
  val mutationTestingElements = "io.stryker-mutator" % "mutation-testing-elements" % versions.mutationTestingElements
  val mutationTestingMetrics =
    "io.stryker-mutator" %% "mutation-testing-metrics-circe" % versions.mutationTestingMetrics
  val pureconfig = "com.github.pureconfig" %% "pureconfig" % versions.pureconfig
  val pureconfigSttp = "com.github.pureconfig" %% "pureconfig-sttp" % versions.pureconfig
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
