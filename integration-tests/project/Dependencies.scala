import sbt._

object Dependencies {

  object Version {
    val cats = "1.5.0"
    val http4s = "0.20.0-M5"
    val scalatest = "3.0.5"
    val Logback = "1.2.3"
    val circeVersion = "0.11.1"
    val mockitoScala = "1.0.6"
  }

  val catsCore = "org.typelevel" %% "cats-core" % Version.cats
  val circeGeneric = "io.circe" %% "circe-generic" % Version.circeVersion
  val http4sServer = "org.http4s" %% "http4s-blaze-server" % Version.http4s
  val http4sCirce = "org.http4s" %% "http4s-circe" % Version.http4s
  val http4sDsl = "org.http4s" %% "http4s-dsl" % Version.http4s
  val logback = "ch.qos.logback" % "logback-classic" % Version.Logback


  object Testing {
    val scalactic = "org.scalactic" %% "scalactic" % Version.scalatest % Test
    val scalatest = "org.scalatest" %% "scalatest" % Version.scalatest % Test
    val mockitoScala = "org.mockito" %% "mockito-scala" % Version.mockitoScala % Test
    val http4sClient = "org.http4s" %% "http4s-blaze-client" % Version.http4s % Test
  }
}
