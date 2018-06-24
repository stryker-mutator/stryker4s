package stryker4s.config

import better.files._

case class Config(files: Seq[String] = Seq("**/main/scala/**/*.scala"),
                  baseDir: File = File.currentWorkingDirectory) {

  override def toString: String =
    s"""stryker4s {
       |  base-dir = ${baseDir.toString()}
       |  files = ${files.mkString("[", ", ", "]")}
       |}""".stripMargin
}
