package stryker4s.testutil

import com.typesafe.config.ConfigFactory
import fs2.io.file.Path
import stryker4s.config.codec.Hocon

/** Example stryker4s configurations for testing purposes
  */
object ExampleConfigs {

  def filled = hoconFor("""stryker4s {
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
                          |}""".stripMargin)

  def empty = hoconFor("")

  def wrongReporter = hoconFor("""stryker4s {
                                 |  reporters: ["dsadsa"]
                                 |}""".stripMargin)

  def overfilled = hoconFor("""stryker4s {
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

  def duplicateKeys = hoconFor("stryker4s.reporters = [html, html]")

  def invalidExcludedMutation =
    hoconFor("stryker4s.excluded-mutations: [Invalid, StillInvalid, BooleanLiteral]")

  def timeoutDuration = hoconFor("""|stryker4s {
                                    | timeout = 6s
                                    |}
                                    |""".stripMargin)

  def hoconFor(content: String) = {
    Hocon.hoconAt(ConfigFactory.parseString(content))("stryker4s", Path("stryker4s.conf"))
  }
}
