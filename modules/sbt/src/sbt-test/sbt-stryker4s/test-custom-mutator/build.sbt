ThisBuild / scalaVersion := "3.3.8"

libraryDependencies ++= Seq(
  "org.scalameta" %% "munit" % "1.3.5" % Test,
  "io.stryker-mutator" %% "stryker4s-mutator-api" % sys.props("plugin.version") % Provided,
  "org.scalameta" %% "scalameta" % "4.17.3" % Provided,
  "org.typelevel" %% "cats-core" % "2.13.0" % Provided
)

strykerDebugLogTestRunnerStdout := true
stryker / logLevel := Level.Debug
strykerCustomMutators := Seq("example.ArithmeticOperatorMutator")
strykerMutate := Seq("src/main/scala/example/Calculator.scala")
strykerReporters := Seq("json")
strykerThresholdsBreak := 50
