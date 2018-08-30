package stryker4s.config.configreader
import com.typesafe.config.{ConfigException, ConfigObject}
import pureconfig.error.{CannotParse, ConfigReaderFailures}
import pureconfig.{ConfigCursor, ConfigReader, SimpleConfigCursor}
import stryker4s.config.{CommandRunner, TestRunner}
import stryker4s.run.process.Command

/** Reads a test-runner from the ConfigReader
  * For now, this is always a command-runner object, but in the future can be expanded to include other test-runners
  */
object TestRunnerReader extends ConfigReader[TestRunner] {
  def from(cur: ConfigCursor): Either[ConfigReaderFailures, TestRunner] =
    cur match {
      case SimpleConfigCursor(value: ConfigObject, _) =>
        try {
          val conf = value.toConfig.getConfig("command-runner")
          val command = conf.getString("command")
          val args = conf.getString("args")
          Right(CommandRunner(command, args))
        } catch {
          case e: ConfigException => cannotParseFailure(e.getMessage, cur)
        }
      case _ => cannotParseFailure(s"Unable to parse test-runner config: $cur", cur)

    }

  private def cannotParseFailure(message: String,
                                 cursor: ConfigCursor): Left[ConfigReaderFailures, TestRunner] = {
    Left(ConfigReaderFailures(CannotParse(message, cursor.location)))
  }
}
