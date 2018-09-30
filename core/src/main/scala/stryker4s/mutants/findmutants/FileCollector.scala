package stryker4s.mutants.findmutants

import better.files._
import stryker4s.config.Config
import stryker4s.run.process.{Command, ProcessRunner}

trait SourceCollector {
  def collectFilesToMutate(): Iterable[File]
  def filesToCopy(processRunner: ProcessRunner): Iterable[File]
}

class FileCollector(implicit config: Config) extends SourceCollector {

  /**
    *  Collect all files that are going to be mutated.
    *
    * @return
    */
  override def collectFilesToMutate(): Iterable[File] = {
    filesToMutate.filterNot(file => filesToExcludeFromMutation.contains(file))
  }

  /**
    * Collect all files that are needed to be copied over to the Stryker4s-tmp folder
    */
  override def filesToCopy(processRunner: ProcessRunner): Iterable[File] = {
    processRunner(Command("git ls-files", "--others --exclude-standard --cached"), config.baseDir)
      .map(config.baseDir / _)
      .distinct
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
