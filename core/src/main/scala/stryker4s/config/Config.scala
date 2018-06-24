package stryker4s.config

import better.files._

case class Config(files: Seq[String] = Seq("**/main/scala/**/*.scala"),
                  excludedFiles: Seq[String] = Seq.empty,
                  baseDir: File = File.currentWorkingDirectory) {

  override def toString: String =
    s"""stryker4s {
       |  base-dir = ${baseDir.toString()}
       |  files = ${files.mkString("[", ", ", "]")}
       |  excluded-files = ${excludedFiles.mkString("[", ", ", "]")}
       |}""".stripMargin
}
