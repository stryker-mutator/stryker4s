package stryker4s.testutil

import java.nio.file.Paths

import pureconfig.ConfigSource

/** Example stryker4s configurations for testing purposes
  */
object ExampleConfigs {

  def filled = ConfigSource.string("""stryker4s {
                                     |  mutate: [
                                     |    "bar/src/main/**/*.scala",
                                     |    "foo/src/main/**/*.scala",
                                     |    "!excluded/file.scala"
                                     |  ]
                                     | 
                                     |  thresholds { high=85, low=65, break=10 }
                                     |
                                     |  base-dir: "/tmp/project"
                                     |
                                     |  reporters: ["html"]
                                     |
                                     |  excluded-mutations: ["BooleanLiteral"]
                                     |  dashboard: {
                                     |    base-url: "https://fakeurl.com"
                                     |    report-type: "mutation-score-only"
                                     |    project: "someProject"
                                     |    version: "someVersion"
                                     |    module: "someModule"
                                     |  }
                                     |  timeout-factor=2.5
                                     |  max-test-runner-reuse=15
                                     |  legacy-test-runner=true
                                     |  timeout=5500
                                     |  scala-dialect="scala212"
                                     |  concurrency = 3
                                     |}""".stripMargin)

  def empty = ConfigSource.empty

  def emptyStryker4s = ConfigSource.string("stryker4s {}")

  def nonExistentFile = ConfigSource.file(Paths.get("nonExistentFile.conf").toAbsolutePath())

  def wrongReporter = ConfigSource.string("""stryker4s {
                                            |  reporters: ["dsadsa"]
                                            |}""".stripMargin)

  def overfilled = ConfigSource.string("""stryker4s {
                                         |  mutate: [
                                         |    "bar/src/main/**/*.scala",
                                         |    "foo/src/main/**/*.scala",
                                         |    "!excluded/file.scala"
                                         |  ]
                                         |
                                         |  base-dir: "/tmp/project"
                                         |
                                         |  reporters: ["html"]
                                         |
                                         |  excluded-mutations: ["BooleanLiteral"]
                                         |
                                         |  unknown-key: "foo"
                                         |  other-unknown-key: "bar"
                                         |}""".stripMargin)

  def duplicateKeys = ConfigSource.string("stryker4s.reporters = [html, html]")

  def invalidExcludedMutation =
    ConfigSource.string("stryker4s.excluded-mutations: [Invalid, StillInvalid, BooleanLiteral]")

  def filledProcess = ConfigSource.string("""stryker4s {
                                            |  test-runner {
                                            |    command = "gradle"
                                            |    args="test"
                                            |  }
                                            |}""".stripMargin)

  def timeoutDuration = ConfigSource.string("""|stryker4s {
                                               | timeout = 6s
                                               |}
                                               |""".stripMargin)

  def scalaDialect(dialect: String) = ConfigSource.string(s"""|stryker4s {
                                                              | scala-dialect="$dialect"
                                                              |}
                                                              |""".stripMargin)
}
