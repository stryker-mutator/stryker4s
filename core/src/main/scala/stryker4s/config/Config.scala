package stryker4s.config

import better.files._
import stryker4s.run.process.Command

case class Config(files: Seq[String] = Seq("**/main/scala/**/*.scala"),
                  baseDir: File = File.currentWorkingDirectory,
                  testRunner: TestRunner = CommandRunner(Command("sbt", "test"))) {
  override def toString: String =
    s"""stryker4s {
       |  base-dir: "$baseDir"
       |  files: ${files.mkString("[\"", "\", \"", "\"]")}
       |  test-runner: $testRunner
       |}""".stripMargin
}
