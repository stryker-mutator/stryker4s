import sbt._

object Dependencies {
  object versions {
    val scala212 = "2.12.10"

    val scalameta = "4.2.4"
    val pureconfig = "0.12.1"
    val scalatest = "3.0.8"
    val mockitoScala = "1.7.1"
    val betterFiles = "3.8.0"
    val log4j = "2.12.1"
    val grizzledSlf4j = "1.3.4"
    val everitJsonSchema = "1.12.0"
    val cats = "2.0.0"
    val circe = "0.12.3"
    val mutationTestingElements = "1.1.1"
    val mutationTestingSchema = "1.1.1"
    val mutationTestingMetrics = "1.2.0"
    val scalajHttp = "2.4.2"
  }

  object test {
    val scalatest = "org.scalatest" %% "scalatest" % versions.scalatest % Test
    val mockitoScala = "org.mockito" %% "mockito-scala-scalatest" % versions.mockitoScala % Test
    val everitJsonSchema = "com.github.everit-org.json-schema" % "org.everit.json.schema" % versions.everitJsonSchema % Test
    val mutationTestingSchema = "io.stryker-mutator" % "mutation-testing-report-schema" % versions.mutationTestingSchema % Test
  }

  val pureconfig = "com.github.pureconfig" %% "pureconfig" % versions.pureconfig
  val scalameta = "org.scalameta" %% "scalameta" % versions.scalameta
  val betterFiles = "com.github.pathikrit" %% "better-files" % versions.betterFiles
  val log4jApi = "org.apache.logging.log4j" % "log4j-api" % versions.log4j
  val log4jCore = "org.apache.logging.log4j" % "log4j-core" % versions.log4j
  val log4jslf4jImpl = "org.apache.logging.log4j" % "log4j-slf4j-impl" % versions.log4j
  val grizzledSlf4j = "org.clapper" %% "grizzled-slf4j" % versions.grizzledSlf4j
  val catsCore = "org.typelevel" %% "cats-core" % versions.cats
  val circeCore = "io.circe" %% "circe-core" % versions.circe
  val scalajHttp = "org.scalaj" %% "scalaj-http" % versions.scalajHttp
  val mutationTestingElements = "io.stryker-mutator" % "mutation-testing-elements" % versions.mutationTestingElements
  val mutationTestingMetrics = "io.stryker-mutator" %% "mutation-testing-metrics-circe" % versions.mutationTestingMetrics
}
