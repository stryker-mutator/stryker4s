resolvers ++= Resolver.sonatypeOssRepos("snapshots")
addSbtPlugin("com.github.sbt" % "sbt-ci-release" % "1.9.2")
addSbtPlugin("io.stryker-mutator" % "sbt-stryker4s" % "0.16.1+169-98795188-SNAPSHOT")
addSbtPlugin("org.typelevel" % "sbt-tpolecat" % "0.5.2")
addSbtPlugin("com.eed3si9n" % "sbt-projectmatrix" % "0.10.1")

// Protobuf plugin and its dependencies
addSbtPlugin("com.thesamet" % "sbt-protoc" % "1.0.7")
libraryDependencies += "com.thesamet.scalapb" %% "compilerplugin" % "0.11.17"
evictionErrorLevel := Level.Info
