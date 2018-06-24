package stryker4s.mutants.findmutants

import better.files._
import stryker4s.config.Config

trait SourceCollector {
  def collectFiles(): Iterable[File]
}

class FileCollector(implicit config: Config) extends SourceCollector {

  private[this] val toMutateFiles: Seq[File] = toFileList(config.files)
  private[this] val toExcludeFiles: Seq[File] = toFileList(config.excludedFiles)

  override def collectFiles(): Iterable[File] = {
    toMutateFiles.filterNot(file => toExcludeFiles.contains(file))
  }

  private[this] def toFileList(files: Seq[String]): Seq[File] = {
    files.flatMap(config.baseDir.glob(_))
  }
}
