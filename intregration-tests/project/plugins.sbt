resolvers += Resolver.sonatypeRepo("snapshots")

addSbtPlugin("org.scoverage" % "sbt-scoverage" % "1.5.1")
addSbtPlugin("com.geirsson" % "sbt-scalafmt" % "1.5.1")
addSbtPlugin("org.lyranthe.sbt" % "partial-unification" % "1.1.2")

addSbtPlugin("io.stryker-mutator" % "sbt-stryker4s" % "0.1.0+16-04e2bac3-SNAPSHOT")