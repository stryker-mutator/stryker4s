resolvers += Resolver.sonatypeRepo("snapshots")

addSbtPlugin("com.geirsson" % "sbt-ci-release" % "1.5.7")
addSbtPlugin("io.stryker-mutator" % "sbt-stryker4s" % "0.11.3+52-ce6329b4-SNAPSHOT")
addSbtPlugin("io.github.davidgregory084" % "sbt-tpolecat" % "0.1.17")
addSbtPlugin("com.eed3si9n" % "sbt-projectmatrix" % "0.8.0")
