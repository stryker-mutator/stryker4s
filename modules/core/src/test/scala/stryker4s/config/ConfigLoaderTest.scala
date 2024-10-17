package stryker4s.config

import cats.effect.IO
import ciris.{ConfigError, ConfigValue}
import fs2.io.file.Path
import stryker4s.config.source.DefaultsConfigSource
import stryker4s.testkit.{LogMatchers, Stryker4sIOSuite}

class ConfigLoaderTest extends Stryker4sIOSuite with LogMatchers {
  test("combines a ConfigSource into Config") {
    val result = ConfigLoader.loadAll[IO](List.empty)
    result.assertEquals(Config.default) *>
      IO(
        assertLoggedInfo(
          "Loading config. Read how to configure Stryker4s here: https://stryker-mutator.io/docs/stryker4s/configuration/"
        )
      )
  }

  test("accumulates failures") {
    val result = new ConfigLoader[IO](new DefaultsConfigSource[IO]() {
      override def baseDir = ConfigValue.failed[Path](ConfigError("baseDir failed")).covary[IO]
      override def mutate = ConfigValue.failed[Seq[String]](ConfigError("mutate failed")).covary[IO]
    }).config.attempt

    result.map(_.leftValue.messages.toList).assertSameElementsAs(List("baseDir failed", "mutate failed"))
  }
}
