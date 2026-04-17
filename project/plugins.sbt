addSbtPlugin("com.github.sbt" % "sbt-ci-release" % "1.11.2")
addSbtPlugin("io.stryker-mutator" % "sbt-stryker4s" % "0.21.0")
addSbtPlugin("org.typelevel" % "sbt-tpolecat" % "0.5.7")

// Protobuf plugin and its dependencies
addSbtPlugin("com.thesamet" % "sbt-protoc" % "1.1.0-RC1")
libraryDependencies += "com.thesamet.scalapb" %% "compilerplugin" % "1.0.0-alpha.5"
resolvers += Resolver.sonatypeCentralSnapshots
libraryDependencies += "com.thesamet.scalapb" %% "protoc-bridge" % "0.9.9+31-85db2060-SNAPSHOT"
