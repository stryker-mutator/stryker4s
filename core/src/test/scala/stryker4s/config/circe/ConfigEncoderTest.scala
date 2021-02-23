package stryker4s.config.circe

import better.files.File
import io.circe.Json._
import io.circe.syntax._
import stryker4s.config._
import stryker4s.testutil.Stryker4sSuite

class ConfigEncoderTest extends Stryker4sSuite {
  describe("configEncoder") {
    it("should be able to encode a minimal config") {
      expectJsonConfig(
        defaultConfig,
        defaultConfigJson,
        """{"mutate":["**/main/scala/**.scala"],"test-filter":[],"base-dir":"/workspace","reporters":["console","html"],"excluded-mutations":[],"thresholds":{"high":80,"low":60,"break":0},"dashboard":{"base-url":"https://dashboard.stryker-mutator.io","report-type":"full"},"timeout":5000,"timeout-factor":1.5,"legacy-test-runner":false,"scala-dialect":"scala3"}"""
      )
    }

    it("should be able to encode a filled config") {
      expectJsonConfig(
        defaultConfig.copy(
          testFilter = Seq("foo.scala"),
          files = Some(Seq("file.scala")),
          excludedMutations = Set("bar.scala"),
          maxTestRunnerReuse = Some(2),
          dashboard = DashboardOptions(
            project = Some("myProject"),
            version = Some("1.3.3.7"),
            module = Some("myModule")
          )
        ),
        defaultConfigJson.mapObject(
          _.add("test-filter", arr(fromString("foo.scala")))
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
        ),
        """{"mutate":["**/main/scala/**.scala"],"test-filter":["foo.scala"],"base-dir":"/workspace","reporters":["console","html"],"files":["file.scala"],"excluded-mutations":["bar.scala"],"thresholds":{"high":80,"low":60,"break":0},"dashboard":{"base-url":"https://dashboard.stryker-mutator.io","report-type":"full","project":"myProject","version":"1.3.3.7","module":"myModule"},"timeout":5000,"timeout-factor":1.5,"max-test-runner-reuse":2,"legacy-test-runner":false,"scala-dialect":"scala3"}"""
      )
    }
  }

  def expectJsonConfig(config: Config, json: io.circe.Json, jsonString: String) = {
    val result = config.asJson

    result.noSpaces shouldBe jsonString
    result shouldBe json
  }

  def defaultConfig: Config = Config.default.copy(baseDir = File("/workspace"))

  def defaultConfigJson = obj(
    "mutate" -> arr(fromString(defaultConfig.mutate.head)),
    "test-filter" -> arr(),
    "base-dir" -> fromString("/workspace"),
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
    "timeout-factor" -> fromDouble(1.5).get,
    "legacy-test-runner" -> False,
    "scala-dialect" -> fromString("scala3")
  )
}
