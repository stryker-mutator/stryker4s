addSbtPlugin("com.github.sbt" % "sbt-ci-release" % "1.11.2")
addSbtPlugin("io.stryker-mutator" % "sbt-stryker4s" % "0.20.3")
// addSbtPlugin("org.typelevel" % "sbt-tpolecat" % "0.5.3")
// addSbtPlugin("com.eed3si9n" % "sbt-projectmatrix" % "0.11.0")

// Protobuf plugin and its dependencies
addSbtPlugin("com.thesamet" % "sbt-protoc" % "1.0.8+4-eb5350f8-SNAPSHOT")
libraryDependencies += "com.thesamet.scalapb" %% "compilerplugin" % "1.0.0-alpha.3+29-3ad1e547-SNAPSHOT"
