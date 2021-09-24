addSbtPlugin("com.github.sbt" % "sbt-ci-release" % "1.5.9")
addSbtPlugin("io.stryker-mutator" % "sbt-stryker4s" % "0.13.1")
addSbtPlugin("io.github.davidgregory084" % "sbt-tpolecat" % "0.1.20")
addSbtPlugin("com.eed3si9n" % "sbt-projectmatrix" % "0.8.0")

// Protobuf plugin and its dependencies
addSbtPlugin("com.thesamet" % "sbt-protoc" % "1.0.4")
libraryDependencies ++= Seq(
  "com.thesamet.scalapb" %% "compilerplugin" % "0.11.5",
  "com.thesamet.scalapb" %% "scalapb-validate-codegen" % "0.3.2"
)
