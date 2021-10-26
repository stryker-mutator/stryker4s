addSbtPlugin("com.github.sbt" % "sbt-ci-release" % "1.5.10")
addSbtPlugin("io.stryker-mutator" % "sbt-stryker4s" % "0.14.0")
addSbtPlugin("io.github.davidgregory084" % "sbt-tpolecat" % "0.1.20")
addSbtPlugin("com.eed3si9n" % "sbt-projectmatrix" % "0.8.0")
addSbtPlugin("org.scala-js" % "sbt-scalajs" % "1.7.1")
addSbtPlugin("org.scalablytyped.converter" % "sbt-converter" % "1.0.0-beta36")

// Protobuf plugin and its dependencies
addSbtPlugin("com.thesamet" % "sbt-protoc" % "1.0.4")
libraryDependencies += "com.thesamet.scalapb" %% "compilerplugin" % "0.11.6"
