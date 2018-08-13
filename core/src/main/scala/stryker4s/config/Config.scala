package stryker4s.config

import better.files._

case class Config(files: Seq[String] = Seq("**/main/scala/**/*.scala"),
                  baseDir: File = File.currentWorkingDirectory,
                  testRunner: TestRunner = CommandRunner("sbt test")) {

  override def toString: String =
    s"""stryker4s {
       |  base-dir: "$baseDir"
       |  files: ${files.mkString("[\"", "\", \"", "\"]")}
       |  test-runner: $runnerString
       |}""".stripMargin

  private def runnerString: String = {
    testRunner match {
      case CommandRunner(command) =>
        s"""{
           |    command-runner: {
           |      command: "$command"
           |    }
           |  }""".stripMargin
    }
  }
}
