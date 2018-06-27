package stryker4s.sbt
import better.files.File
import sbt._
import sbt.Keys._
import sbt.{File => SbtFile}
import stryker4s.mutants.findmutants.SourceCollector

class SbtSourceCollector(filesTask: TaskKey[Seq[SbtFile]]) extends SourceCollector {
  override def collectFiles(): Iterable[File] = {

    filesTask.map(seq => seq.map(file => File(file.getAbsolutePath))).value
  }
}
