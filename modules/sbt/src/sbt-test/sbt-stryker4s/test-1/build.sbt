scalaVersion := "2.12.20"

libraryDependencies += "org.scalameta" %% "munit" % "1.0.4" % Test

stryker / logLevel := Level.Debug

// Reproduce https://github.com/stryker-mutator/stryker4s/issues/726
(Compile / scalaSource) := baseDirectory.value / "src" / "main" / "scala"
(Compile / javaSource) := baseDirectory.value / "src" / "main" / "scala"
