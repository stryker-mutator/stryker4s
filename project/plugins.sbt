addSbtPlugin("com.github.sbt" % "sbt-ci-release" % "1.5.12")
addSbtPlugin("io.stryker-mutator" % "sbt-stryker4s" % "0.14.3")
addSbtPlugin("org.typelevel" % "sbt-tpolecat" % "0.5.0")
addSbtPlugin("com.eed3si9n" % "sbt-projectmatrix" % "0.9.1")

// Protobuf plugin and its dependencies
addSbtPlugin("com.thesamet" % "sbt-protoc" % "1.0.6")
libraryDependencies += "com.thesamet.scalapb" %% "compilerplugin" % "0.11.13"
