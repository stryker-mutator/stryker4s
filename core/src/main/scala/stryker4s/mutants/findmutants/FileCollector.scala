package stryker4s.mutants.findmutants

import better.files._
import stryker4s.config.Config

trait SourceCollector {
  def collectFiles(): Iterable[File]
}

class FileCollector(implicit config: Config) extends SourceCollector {

  override def collectFiles(): Iterable[File] = {
    filesToMutate.filterNot(file => filesToExclude.contains(file))
  }

  private[this] val filesToMutate: Seq[File] = {
    config.files
      .filterNot(file => file.startsWith("!"))
      .flatMap(config.baseDir.glob(_))
      .distinct
  }

  private[this] val filesToExclude: Seq[File] = {
    config.files
      .filter(file => file.startsWith("!"))
      .flatMap(file => config.baseDir.glob(file.stripPrefix("!")))
      .distinct
  }
}
