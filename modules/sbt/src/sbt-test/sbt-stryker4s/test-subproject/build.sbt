// The whole point of this scripted test: the project to be mutated is in `file("app")`,
// not at the build root. With the pre-fix plugin, `sbt app/stryker` resolved task queries
// against the auto-generated root (no test framework) → 0% mutation score.

// Scala 3 LTS so we can exercise the -Werror stripping path: -Werror is what
// tpolecat 0.5.3+ emits for Scala 3 (replacing the legacy -Xfatal-warnings).
// This is also the path the original bug surfaced on (Scala 3.8.3 in a real-world
// build, binary-compatible with 3.3.x).
ThisBuild / scalaVersion := "3.3.7"

lazy val app = (project in file("app"))
  .settings(
    libraryDependencies += "org.scalameta" %% "munit" % "1.3.0" % Test,
    strykerDebugLogTestRunnerStdout := true,
    stryker / logLevel := Level.Debug,
    // Set thresholds via sbt settings rather than stryker4s.conf so they apply in both
    // sbt 1.x and sbt 2.x scripted runs (the file-based config didn't get picked up under
    // sbt 2 in this layout). The break threshold is the actual regression guard: if the
    // subproject-scoping bug returns, the score collapses to 0% and `app/stryker` fails.
    strykerThresholdsBreak := 50,
    // Exercises the fatal-warning stripping path: without it, warnings the synthetic
    // `case Some("1") => ...` mutation-switching pattern emits (e.g. non-exhaustive match)
    // would be promoted to errors → compilation fails → all mutants get rolled back → 0%.
    // Both flags appear in real-world builds (tpolecat 0.5.3+ emits -Werror for Scala 3;
    // -Xfatal-warnings is the legacy 2.x flag) so the test pins both.
    scalacOptions ++= Seq("-Werror", "-Xfatal-warnings")
  )
