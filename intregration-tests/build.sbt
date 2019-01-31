lazy val root = (project in file("."))
  .settings(
    organization := "io.stryker4s",
    name := "intregration-tests",
    scalaVersion := "2.12.8",
    libraryDependencies ++= Seq(
      Dependencies.http4sServer,
      Dependencies.http4sCirce,
      Dependencies.http4sDsl,
      Dependencies.circeGeneric,
      Dependencies.logback,
      Dependencies.Test.scalatest,
      Dependencies.Test.scalactic,
      Dependencies.Test.mockitoScala,
    ),
    addCompilerPlugin("org.spire-math" %% "kind-projector" % "0.9.6"),
    addCompilerPlugin("com.olegpy" %% "better-monadic-for" % "0.2.4")
  )