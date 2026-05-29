ThisBuild / scalaVersion := "3.3.7"

lazy val app = (project in file("app"))
  .settings(
    libraryDependencies += "org.scalameta" %% "munit" % "1.3.1" % Test,
    strykerDebugLogTestRunnerStdout := true,
    stryker / logLevel := Level.Debug,
    strykerThresholdsBreak := 50
  )
