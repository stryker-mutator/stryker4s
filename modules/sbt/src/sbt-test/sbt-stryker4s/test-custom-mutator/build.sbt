ThisBuild / scalaVersion := "3.9.0"

libraryDependencies ++= Seq(
  "org.scalameta" %% "munit" % "1.3.5" % Test,
  "io.stryker-mutator" %% "stryker4s-plugin-api" % sys.props("plugin.version") % Provided,
  "org.scalameta" %% "scalameta" % "4.17.3" % Provided,
  "org.typelevel" %% "cats-core" % "2.13.0" % Provided
)

strykerDebugLogTestRunnerStdout := true
stryker / logLevel := Level.Debug

strykerMutate := Seq("src/main/scala/example/Calculator.scala")
strykerReporters := Seq("json")
strykerThresholdsBreak := 50

val checkReport = inputKey[Unit]("Check that custom mutants were generated and killed")

checkReport := {
  val reportDir = baseDirectory.value / "target" / "stryker4s-report"
  val report = (reportDir ** "report.json")
    .get()
    .sortBy(_.lastModified())
    .lastOption
    .getOrElse(sys.error(s"No report.json found under $reportDir"))
  val contents = IO.read(report)
  assert(contents.contains("ArithmeticOperator"), s"No ArithmeticOperator mutants in $report")
  Seq("CompileError", "Survived").foreach { status =>
    assert(!contents.contains("\"" + status + "\""), s"Found $status mutants in $report, expected them to be killed")
  }
}
