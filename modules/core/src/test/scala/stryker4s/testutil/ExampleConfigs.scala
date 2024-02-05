package stryker4s.testutil

/** Example stryker4s configurations for testing purposes
  */
object ExampleConfigs {

  def filled = """stryker4s {
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
                 |  debug {
                 |    log-test-runner-stdout=true
                 |    debug-test-runner=true
                 |  }
                 |  static-tmp-dir = true
                 |  clean-tmp-dir = false
                 |}""".stripMargin

  def empty = ""

  def wrongReporter = """stryker4s {
                        |  reporters: ["dsadsa"]
                        |}""".stripMargin

  def overfilled = """stryker4s {
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
                     |}""".stripMargin

  def duplicateKeys = "stryker4s.reporters = [html, html]"

  def invalidExcludedMutation =
    "stryker4s.excluded-mutations: [Invalid, StillInvalid, BooleanLiteral]"

  def timeoutDuration = """|stryker4s {
                           | timeout = 6s
                           |}
                           |""".stripMargin

  def scalaDialect(dialect: String) = s"""|stryker4s {
                                          | scala-dialect="$dialect"
                                          |}
                                          |""".stripMargin
}
