ThisBuild / scalaVersion := "3.3.8"

lazy val app = (project in file("app"))
  .settings(
    libraryDependencies += "org.scalameta" %% "munit" % "1.3.4" % Test,
    strykerDebugLogTestRunnerStdout := true,
    stryker / logLevel := Level.Debug,
    strykerThresholdsBreak := 50
  )
