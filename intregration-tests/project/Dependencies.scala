import sbt._

object Dependencies {

  object Version {
    val http4s = "0.18.21"
    val scalatest = "3.0.5"
    val Logback = "1.2.3"
    val circeVersion = "0.11.1"
    val mockitoScala = "1.0.6"
  }

  val http4sServer = "org.http4s" %% "http4s-blaze-server" % Version.http4s
  val http4sCirce = "org.http4s" %% "http4s-circe" % Version.http4s
  val http4sDsl = "org.http4s" %% "http4s-dsl" % Version.http4s
  val logback = "ch.qos.logback" % "logback-classic" % Version.Logback
  val circeGeneric = "io.circe" %% "circe-generic" % Version.circeVersion

  object Test {
    val scalactic = "org.scalactic" %% "scalactic" % Version.scalatest % "test"
    val scalatest = "org.scalatest" %% "scalatest" % Version.scalatest % "test"
    val mockitoScala = "org.mockito" %% "mockito-scala" % Version.mockitoScala % "test"
  }

}
