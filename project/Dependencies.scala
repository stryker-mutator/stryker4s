import sbt._

object Dependencies {

  object versions {
    val scala212 = "2.12.7"
    val scala211 = "2.11.12"
    val crossScala = Seq(scala211, scala212)

    val scalameta = "4.0.0"
    val pureconfig = "0.9.2"
    val scalatest = "3.0.5"
    val mockitoScala = "0.4.5"
    val betterFiles = "3.6.0"
    val log4j = "2.11.1"
    val grizzledSlf4j = "1.3.2"
  }

  object test {
    val scalatest = "org.scalatest" %% "scalatest" % versions.scalatest % Test
    val mockitoScala = "org.mockito" %% "mockito-scala" % versions.mockitoScala % Test
  }

  val pureconfig = "com.github.pureconfig" %% "pureconfig" % versions.pureconfig
  val scalameta = "org.scalameta" %% "scalameta" % versions.scalameta
  val scalametaContrib = "org.scalameta" %% "contrib" % versions.scalameta
  val betterFiles = "com.github.pathikrit" %% "better-files" % versions.betterFiles
  val log4jApi = "org.apache.logging.log4j" % "log4j-api" % versions.log4j
  val log4jCore = "org.apache.logging.log4j" % "log4j-core" % versions.log4j
  val log4jslf4jImpl = "org.apache.logging.log4j" % "log4j-slf4j-impl" % versions.log4j
  val grizzledSlf4j = "org.clapper" %% "grizzled-slf4j" % versions.grizzledSlf4j

}
