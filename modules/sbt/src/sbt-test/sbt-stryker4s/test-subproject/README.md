# SBT plugin test project — subproject layout

Regression test for the case where the project being mutated lives in a
subdirectory (`file("app")`) rather than at the build root. The bug:
`sbt-stryker4s` queried `Test / loadedTestFrameworks`, `Test / fullClasspath`,
`Compile / compile`, etc. without scoping them to the calling project, so
`sbt app/stryker` resolved them against the auto-generated root project
(which has no test framework) → empty frameworks → silent 0% mutation
score.

The fix: pass `thisProjectRef` from the task macro into `Stryker4sSbtRunner`
and scope every task query / appended setting to it. If the fix regresses,
the `strykerThresholdsBreak` set in `build.sbt` trips and this scripted
test fails.
