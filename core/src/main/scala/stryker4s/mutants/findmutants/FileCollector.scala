package stryker4s.mutants.findmutants

import better.files._
import stryker4s.config.Config

trait SourceCollector {
  def collectFilesToMutate(): Iterable[File]
}

class FileCollector(implicit config: Config) extends SourceCollector {

  override def collectFilesToMutate(): Iterable[File] = {
    filesToMutate.filterNot(file => filesToExcludeFromMutation.contains(file))
  }

  private[this] val filesToMutate: Seq[File] = {
    config.mutate
      .filterNot(file => file.startsWith("!"))
      .flatMap(config.baseDir.glob(_))
      .distinct
  }

  private[this] val filesToExcludeFromMutation: Seq[File] = {
    config.mutate
      .filter(file => file.startsWith("!"))
      .flatMap(file => config.baseDir.glob(file.stripPrefix("!")))
      .distinct
  }
}
