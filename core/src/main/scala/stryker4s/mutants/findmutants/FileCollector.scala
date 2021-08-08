package stryker4s.mutants.findmutants

import better.files.File
import fs2.io.file.Path
import stryker4s.config.Config
import stryker4s.extension.FileExtensions.PathExtensions
import stryker4s.log.Logger
import stryker4s.run.process.{Command, ProcessRunner}

import scala.util.{Failure, Success}

trait SourceCollector {
  def collectFilesToMutate(): Iterable[Path]
  def filesToCopy: Iterable[Path]
}

/** TODO: rewrite this to use fs2.io.file.Path internally as well
  */
class FileCollector(private[this] val processRunner: ProcessRunner)(implicit config: Config, log: Logger)
    extends SourceCollector {

  /** Get path separator because windows and unix systems have different separators.
    */
  private[this] val pathSeparator = config.baseDir.toNioPath.getFileSystem().getSeparator

  /** Collect all files that are going to be mutated.
    *
    * Options are:
    *   - Files that are configured to be excluded
    *   - in the target folder
    *   - directories are skipped
    */
  override def collectFilesToMutate(): Iterable[Path] = {
    filesToMutate
      .filterNot(filesToExcludeFromMutation.contains(_))
      .filterNot(isInTargetDirectory)
      .filterNot(_.isDirectory)
      .map(_.path)
      .map(Path.fromNioPath)
  }

  /** Collect all files that are needed to be copied over to the Stryker4s-tmp folder.
    *
    *   - Option 1: Copy every file that is listed by the 'files' config setting.
    *   - Option 2: Copy every file that is listed by git.
    *   - Option 3: Copy every file in the 'baseDir' excluding target folders.
    */
  override def filesToCopy: Iterable[Path] = {
    (listFilesBasedOnConfiguration() orElse
      listFilesBasedOnGit(processRunner) getOrElse
      listAllFiles())
      .filterNot(isInTargetDirectory)
      .filterNot(_.isDirectory)
      .filter(_.exists)
      .map(_.path)
      .map(Path.fromNioPath)
  }

  /** List all files based on the 'files' configuration key from stryker4s.conf.
    */
  private[this] def listFilesBasedOnConfiguration(): Option[Iterable[File]] = {
    config.files.map(glob)
  }

  /** List all files based on `git ls-files` command.
    */
  private[this] def listFilesBasedOnGit(processRunner: ProcessRunner): Option[Iterable[File]] = {
    processRunner(
      Command("git ls-files", "--others --exclude-standard --cached"),
      config.baseDir
    ) match {
      case Success(files) => Option(files.map(File(config.baseDir.toNioPath) / _).distinct)
      case Failure(_)     => None
    }
  }

  /** List all files from the base directory specified in the Stryker4s basedir config key.
    */
  private[this] def listAllFiles(): Iterable[File] = {
    log.warn("No 'files' specified and not a git repository.")
    log.warn("Falling back to copying everything except the 'target/' folder(s)")

    File(config.baseDir.toNioPath).listRecursively.toSeq
  }

  private[this] def glob(list: Seq[String]): Seq[File] = {
    list
      .flatMap(File(config.baseDir.toNioPath).glob(_))
      .distinct
  }

  private[this] val filesToMutate: Seq[File] = glob(
    config.mutate.filterNot(file => file.startsWith("!"))
  )

  private[this] val filesToExcludeFromMutation: Seq[File] = glob(
    config.mutate
      .filter(file => file.startsWith("!"))
      .map(file => file.stripPrefix("!"))
  )

  /** Is the file in the target folder, and thus should not be copied over
    */
  private[this] def isInTargetDirectory(file: File): Boolean = {
    val relativePath = Path.fromNioPath(file.path).relativePath.toString

    (file.isDirectory && relativePath == "target") ||
    relativePath.startsWith(s"target$pathSeparator") ||
    relativePath.contains(s"${pathSeparator}target$pathSeparator") ||
    relativePath.endsWith(s"${pathSeparator}target")
  }
}
