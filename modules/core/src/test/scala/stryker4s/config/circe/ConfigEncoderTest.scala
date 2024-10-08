package stryker4s.config.circe

import cats.syntax.option.*
import fs2.io.file.Path
import io.circe.Json.*
import io.circe.syntax.*
import munit.Location
import stryker4s.config.*
import stryker4s.config.codec.CirceConfigEncoder
import stryker4s.testkit.Stryker4sSuite

class ConfigEncoderTest extends Stryker4sSuite with CirceConfigEncoder {
  val workspaceLocation = Path("workspace").absolute.toString
  describe("configEncoder") {
    test("should be able to encode a minimal config") {
      expectJsonConfig(
        defaultConfig,
        defaultConfigJson,
        s"""{"mutate":["**/main/scala/**.scala"],"test-filter":[],"base-dir":"${workspaceLocation.replace(
            "\\",
            "\\\\"
          )}","reporters":["console","html"],"files":["**/main/scala/**.scala"],"excluded-mutations":[],"thresholds":{"high":80,"low":60,"break":0},"dashboard":{"base-url":"https://dashboard.stryker-mutator.io","report-type":"full"},"timeout":5000,"timeout-factor":1.5,"legacy-test-runner":false,"scala-dialect":"scala213source3","debug":{"log-test-runner-stdout":false,"debug-test-runner":false}}"""
      )
    }

    test("should be able to encode a filled config") {
      expectJsonConfig(
        defaultConfig.copy(
          mutate = Seq("**/main/scala/**.scala"),
          testFilter = Seq("foo.scala"),
          files = Seq("file.scala"),
          excludedMutations = Seq(ExcludedMutation("bar.scala")),
          maxTestRunnerReuse = 2.some,
          dashboard = Config.default.dashboard.copy(
            project = "myProject".some,
            version = "1.3.3.7".some,
            module = "myModule".some
          ),
          debug = DebugOptions(
            logTestRunnerStdout = true,
            debugTestRunner = true
          )
        ),
        defaultConfigJson.mapObject(
          _.add("mutate", arr(fromString("**/main/scala/**.scala")))
            .add("test-filter", arr(fromString("foo.scala")))
            .add("files", arr(fromString("file.scala")))
            .add("excluded-mutations", arr(fromString("bar.scala")))
            .add("max-test-runner-reuse", fromInt(2))
            .add(
              "dashboard",
              obj(
                "base-url" -> fromString(defaultConfig.dashboard.baseUrl.toString()),
                "report-type" -> fromString("full"),
                "project" -> fromString("myProject"),
                "version" -> fromString("1.3.3.7"),
                "module" -> fromString("myModule")
              )
            )
            .add(
              "debug",
              obj(
                "log-test-runner-stdout" -> fromBoolean(true),
                "debug-test-runner" -> fromBoolean(true)
              )
            )
        ),
        s"""{"mutate":["**/main/scala/**.scala"],"test-filter":["foo.scala"],"base-dir":"${workspaceLocation.replace(
            "\\",
            "\\\\"
          )}","reporters":["console","html"],"files":["file.scala"],"excluded-mutations":["bar.scala"],"thresholds":{"high":80,"low":60,"break":0},"dashboard":{"base-url":"https://dashboard.stryker-mutator.io","report-type":"full","project":"myProject","version":"1.3.3.7","module":"myModule"},"timeout":5000,"timeout-factor":1.5,"max-test-runner-reuse":2,"legacy-test-runner":false,"scala-dialect":"scala213source3","debug":{"log-test-runner-stdout":true,"debug-test-runner":true}}"""
      )
    }
  }

  def expectJsonConfig(config: Config, json: io.circe.Json, jsonString: String)(implicit loc: Location) = {
    val result = config.asJson

    assertNoDiff(result.noSpaces, jsonString)
    assertEquals(result, json)
  }

  def defaultConfig: Config = Config.default.copy(baseDir = Path("workspace"))

  def defaultConfigJson = obj(
    "mutate" -> arr(fromString("**/main/scala/**.scala")),
    "files" -> arr(fromString("**/main/scala/**.scala")),
    "test-filter" -> arr(),
    "base-dir" -> fromString(workspaceLocation),
    "reporters" -> arr(fromString("console"), fromString("html")),
    "excluded-mutations" -> arr(),
    "thresholds" -> obj(
      "high" -> fromInt(80),
      "low" -> fromInt(60),
      "break" -> fromInt(0)
    ),
    "dashboard" -> obj(
      "base-url" -> fromString(defaultConfig.dashboard.baseUrl.toString()),
      "report-type" -> fromString("full")
    ),
    "timeout" -> fromInt(5000),
    "timeout-factor" -> fromDouble(1.5).value,
    "legacy-test-runner" -> False,
    "scala-dialect" -> fromString("scala213source3"),
    "debug" -> obj(
      "log-test-runner-stdout" -> False,
      "debug-test-runner" -> False
    )
  )
}
