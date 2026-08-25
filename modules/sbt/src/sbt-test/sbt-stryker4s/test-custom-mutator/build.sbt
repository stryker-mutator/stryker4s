ThisBuild / scalaVersion := "3.3.8"

libraryDependencies ++= Seq(
  "org.scalameta" %% "munit" % "1.3.5" % Test,
  "org.typelevel" %% "munit-cats-effect" % "2.1.0" % Test,
  "io.stryker-mutator" %% "stryker4s-mutator-api" % sys.props("plugin.version") % Provided,
  "org.scalameta" %% "scalameta" % "4.17.3" % Provided,
  "org.typelevel" %% "cats-core" % "2.13.0" % Provided,
  "org.typelevel" %% "cats-effect" % "3.7.0"
)

strykerDebugLogTestRunnerStdout := true
stryker / logLevel := Level.Debug

strykerCustomMutators := Seq(
  "example.ArithmeticOperatorMutator",
  "example.NumericLiteralMutator",
  "example.CollectionLiteralMutator",
  "example.ErrorHandlingMutator",
  "example.ConditionalEffectMutator",
  "example.FallbackRemovalMutator",
  "example.FallbackInversionMutator",
  "example.SequencingSwapMutator",
  "example.SequencingRemovalMutator"
)

strykerMutate := Seq("src/main/scala/example/Calculator.scala")
strykerReporters := Seq("json")
strykerThresholdsBreak := 50
