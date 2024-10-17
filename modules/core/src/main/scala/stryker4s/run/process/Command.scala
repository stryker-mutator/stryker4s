package stryker4s.run.process

/** Used by command-runner module to run a test command with arguments
  */
final case class Command(command: String, args: String)
