import sbt._

object Dependencies {

  object versions {
    val scala212 = "2.12.6"
    val scala211 = "2.11.12"
    val crossScala = Seq(scala211, scala212)

    /** Use 3.3.1 until a Scalameta bug with transforming a Scalameta Parsed is fixed
      * See: https://github.com/scalameta/scalameta/issues/1526
      */
    val scalameta = "3.3.1"
    val pureconfig = "0.9.2"
    val scalatest = "3.0.5"
    val mockitoScala = "0.4.2"
    val betterFiles = "3.5.0"
    val logback = "1.2.3"
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
  val logback = "ch.qos.logback" % "logback-classic" % versions.logback
  val grizzledSlf4j = "org.clapper" %% "grizzled-slf4j" % versions.grizzledSlf4j

}
