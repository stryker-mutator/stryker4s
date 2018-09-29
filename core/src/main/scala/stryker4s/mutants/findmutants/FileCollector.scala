package stryker4s.mutants.findmutants

import better.files._
import stryker4s.config.Config
import stryker4s.run.process.{Command, ProcessRunner}

trait SourceCollector {
  def collectFiles(): Iterable[File]
  def filesToCopy(processRunner: ProcessRunner): Iterable[File]
}

class FileCollector(implicit config: Config) extends SourceCollector {

  def collectFiles(): Iterable[File] = {
    filesToMutate.filterNot(file => filesToExclude.contains(file))
  }

  /**
    * Get all files that are needed to be copied over to the Stryker4s-tmp folder
    */
  def filesToCopy(processRunner: ProcessRunner): Iterable[File] = {
    processRunner(Command("git ls-files", "--others --exclude-standard --cached"), config.baseDir)
      .map(File(_))
      .distinct
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
