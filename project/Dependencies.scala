import sbt._

object Dependencies {

  object versions {
    val scala212 = "2.12.8"

    val scalameta = "4.1.12"
    val pureconfig = "0.11.1"
    val scalatest = "3.0.8"
    val mockitoScala = "1.5.11"
    val betterFiles = "3.8.0"
    val log4j = "2.11.2"
    val grizzledSlf4j = "1.3.4"
    val everitJsonSchema = "1.11.1"
    val circe = "0.11.1"
    val mutationTestingElements = "1.1.0"
    val mutationTestingSchema = "1.1.0"
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
  val circeCore = "io.circe" %% "circe-core" % versions.circe
  val circeGeneric = "io.circe" %% "circe-generic" % versions.circe
  val scalajHttp = "org.scalaj" %% "scalaj-http" % versions.scalajHttp
  val mutationTestingElements = "io.stryker-mutator" % "mutation-testing-elements" % versions.mutationTestingElements

}
