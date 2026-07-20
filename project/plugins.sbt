addSbtPlugin("com.github.sbt" % "sbt-ci-release" % "1.12.0")
addSbtPlugin("io.stryker-mutator" % "sbt-stryker4s" % "1.0.3")
addSbtPlugin("org.typelevel" % "sbt-tpolecat" % "0.5.7")

// Protobuf plugin and its dependencies
addSbtPlugin("com.thesamet" % "sbt-protoc" % "1.1.0-RC2")
libraryDependencies += "com.thesamet.scalapb" %% "compilerplugin" % "1.0.0-alpha.6"
