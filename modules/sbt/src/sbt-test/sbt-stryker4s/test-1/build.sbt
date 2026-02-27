scalaVersion := "2.12.21"

libraryDependencies += "org.scalameta" %% "munit" % "1.2.3" % Test

strykerDebugLogTestRunnerStdout := true
stryker / logLevel := Level.Debug

// Reproduce https://github.com/stryker-mutator/stryker4s/issues/726
(Compile / scalaSource) := baseDirectory.value / "src" / "main" / "scala"
(Compile / javaSource) := baseDirectory.value / "src" / "main" / "scala"
