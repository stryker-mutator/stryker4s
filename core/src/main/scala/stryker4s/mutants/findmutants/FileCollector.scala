package stryker4s.mutants.findmutants

import better.files._
import grizzled.slf4j.Logging
import stryker4s.config.Config
import stryker4s.run.process.{Command, ProcessRunner}

import scala.util.{Failure, Success}

trait SourceCollector {
  def collectFilesToMutate(): Iterable[File]
  def filesToCopy(processRunner: ProcessRunner): Iterable[File]
}

class FileCollector(implicit config: Config) extends SourceCollector with Logging {

  /**
    *  Collect all files that are going to be mutated.
    */
  override def collectFilesToMutate(): Iterable[File] = {
    filesToMutate
      .filterNot(filesToExcludeFromMutation.contains(_))
      .filterNot(stryker4sTmpFiles.contains(_))
  }

  /**
    * Collect all files that are needed to be copied over to the Stryker4s-tmp folder.
    *
    * Option 1: Copy every file that is listed by git.
    * Option 2: Copy every file that is listed by the 'files' config setting
    * Option 3: Copy every file in the 'baseDir' excluding target folders.
    */
  override def filesToCopy(processRunner: ProcessRunner): Iterable[File] = {
    processRunner(Command("git ls-files", "--others --exclude-standard --cached"), config.baseDir) match {
      case Success(files) => files.map(config.baseDir / _).distinct
      case Failure(_) =>
        info("Not a git repo falling back to 'files' configuration.")
        fallBackToFilesConfiguration()
    }
  }

  private[this] def fallBackToFilesConfiguration(): Seq[File] = {
    config.files match {
      case Some(files) => glob(files)
      case None =>
        warn("No 'files' specified falling back to copying everything excluding target")

        config.baseDir
          .glob("**/*.*")
          .filterNot(file => file.path.toString.contains("target"))
          .toSeq ++: config.baseDir.glob("*.*").toSeq
    }
  }

  private[this] def glob(list: Seq[String]): Seq[File] = {
    list
      .flatMap(config.baseDir.glob(_))
      .distinct
  }

  private[this] val filesToMutate: Seq[File] = {
    glob(config.mutate.filterNot(file => file.startsWith("!")))
  }

  private[this] val filesToExcludeFromMutation: Seq[File] = {
    glob(config.mutate
      .filter(file => file.startsWith("!"))
      .map(file => file.stripPrefix("!")))
  }

  /**
    * List of all previously copied files if the target folder is not cleaned.
    */
  private[this] val stryker4sTmpFiles: Seq[File] = {
    config.baseDir
      .glob("target/stryker4s-*")
      .flatMap(_.listRecursively)
      .toSeq
  }
}
