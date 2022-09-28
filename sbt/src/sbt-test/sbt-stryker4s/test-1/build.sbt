scalaVersion := "2.12.17"

libraryDependencies += "org.scalatest" %% "scalatest" % "3.2.13" % Test // scala-steward:off

// Reproduce https://github.com/stryker-mutator/stryker4s/issues/726
(Compile / scalaSource) := baseDirectory.value / "src" / "main" / "scala"
(Compile / javaSource) := baseDirectory.value / "src" / "main" / "scala"
