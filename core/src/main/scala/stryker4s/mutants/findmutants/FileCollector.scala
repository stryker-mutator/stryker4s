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
    * Get path separator because windows and unix systems have different separators.
    */
  private[this] val pathSeparator = config.baseDir.fileSystem.getSeparator

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
    * Option 1: Copy every file that is listed by the 'files' config setting
    * Option 2: Copy every file that is listed by git.
    * Option 3: Copy every file in the 'baseDir' excluding target folders.
    */
  override def filesToCopy(processRunner: ProcessRunner): Iterable[File] = {
    (listFilesBasedOnConfiguration() orElse listFilesBasedOnGit(processRunner) orElse {
      warn("No 'files' specified and not a git repository.")
      warn("Falling back to copying everything except the target/ folder(s)")

      listAllFiles()
    }).getOrElse(Seq.empty)
  }

  /**
    * List all files based on the 'files' configuration key from stryker4s.conf.
    */
  private[this] def listFilesBasedOnConfiguration(): Option[Iterable[File]] = {
    debug("Collecting files based on 'files' configuration key.")
    config.files.map { glob }
  }

  /**
    * List all files based on `git ls-files` command.
    */
  private[this] def listFilesBasedOnGit(processRunner: ProcessRunner): Option[Iterable[File]] = {
    debug("Collecting files based on 'git ls-files'.")
    processRunner(Command("git ls-files", "--others --exclude-standard --cached"), config.baseDir) match {
      case Success(files) => Option(files.map(config.baseDir / _).distinct)
      case Failure(_)     => None
    }
  }

  /**
    * List all files from the base directory specified in the Stryker4s basedir config key.
    */
  private[this] def listAllFiles(): Option[Iterable[File]] = {
    Option(
      (config.baseDir.glob("*.*") ++
        config.baseDir
          .glob("**/*.*")
          .filterNot(file => file.pathAsString.contains(s"${pathSeparator}target$pathSeparator"))).toIterable)
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
    glob(
      config.mutate
        .filter(file => file.startsWith("!"))
        .map(file => file.stripPrefix("!")))
  }

  /**
    * List of all previously copied files if the target folder is not cleaned.
    */
  private[this] val stryker4sTmpFiles: Seq[File] = {
    config.baseDir
      .glob(s"target${pathSeparator}stryker4s-*")
      .flatMap(_.listRecursively)
      .toSeq
  }
}
