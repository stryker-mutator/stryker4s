package stryker4s.config
import stryker4s.run.process.Command

trait TestRunner

case class CommandRunner(command: Command) extends TestRunner {
  override def toString: String = s"""{
                                     |    command-runner: {
                                     |      command: "${command.command}",
                                     |      args: "${command.args}"
                                     |    }
                                     |  }""".stripMargin
}
