resolvers += Resolver.mavenLocal

sys.props.get("plugin.version") match {
  case Some(x) => addSbtPlugin("io.stryker-mutator" % "sbt-stryker4jvm" % x)
  case _ => sys.error("""|The system property 'plugin.version' is not defined.
                         |Specify this property using the scriptedLaunchOpts -D.""".stripMargin)
}
