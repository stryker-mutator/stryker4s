addSbtPlugin("org.scoverage" % "sbt-scoverage" % "1.5.1")
addSbtPlugin("com.geirsson" % "sbt-scalafmt" % "1.5.1")

/**
  * It can be replaced by enabling SbtPlugin since sbt 1.2.0
  */
libraryDependencies += {
  "org.scala-sbt" %% "scripted-plugin" % sbtVersion.value
}