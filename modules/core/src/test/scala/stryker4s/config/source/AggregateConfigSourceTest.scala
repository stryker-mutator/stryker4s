package stryker4s.config.source

import cats.data.NonEmptyList
import cats.effect.IO
import ciris.ConfigValue
import fansi.Color
import fs2.io.file.Path
import stryker4s.testkit.{LogMatchers, Stryker4sIOSuite}
import stryker4s.testutil.ExampleConfigs

import scala.concurrent.duration.*

class AggregateConfigSourceTest extends Stryker4sIOSuite with LogMatchers {
  test("combines multiple sources") {
    val file = new FileConfigSource[IO](ConfigValue.default(ExampleConfigs.timeoutDuration))
    val path = Path("/tmp/project")
    val cli = new CliConfigSource[IO](Seq("--base-dir=/tmp/project"))
    val defaults = new DefaultsConfigSource[IO]()

    val aggregate = new AggregateConfigSource[IO](NonEmptyList.of(file, cli, defaults))

    // file
    aggregate.timeout.load.assertEquals(6.seconds) *>
      IO(assertLoggedDebug(s"Loaded ${Color.Magenta("timeout")} from ${Color.Cyan("file config")}: 6 seconds")) *>
      // cli
      aggregate.baseDir.load.assertEquals(Path("/tmp/project")) *>
      IO(assertLoggedDebug(s"Loaded ${Color.Magenta("baseDir")} from ${Color.Cyan("CLI arguments")}: $path")) *>
      // defaults
      aggregate.thresholdsHigh.load.assertEquals(80) *>
      IO(assertLoggedDebug(s"Loaded ${Color.Magenta("thresholdsHigh")} from ${Color.Cyan("defaults")}: 80"))
  }

  test("aggregate combines given sources and defaults") {
    val cli = new CliConfigSource[IO](Seq("--base-dir=/tmp/project"))
    ConfigSource.aggregate(List(cli)).flatMap { conf =>
      conf.baseDir.load.assertEquals(Path("/tmp/project")) *>
        conf.thresholdsHigh.load.assertEquals(80)
    } *> IO(
      assertLoggedDebug(
        s"Loaded config sources '${Color.Cyan("CLI arguments")}', '${Color.Cyan("file config")}'"
      )
    )
  }
}
