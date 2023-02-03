package stryker4jvm.files

import cats.effect.IO
import fs2.Stream
import fs2.io.file.{Files, Path}
import stryker4jvm.config.Config
import stryker4jvm.files.Glob.glob
import stryker4jvm.logging.FansiLogger
import stryker4jvm.run.process.{Command, ProcessRunner}

class ConfigFilesResolver(processRunner: ProcessRunner)(implicit config: Config, log: FansiLogger)
    extends FilesFileResolver {

  /** Collect all files that are needed to be copied over to the Stryker4jvm-tmp folder.
    *
    *   - Option 1: Copy every file that is listed by the 'files' config setting.
    *   - Option 2: Copy every file that is listed by git.
    *   - Option 3: Copy every file in the 'baseDir' excluding target folders.
    */
  override def files: Stream[IO, Path] =
    (listFilesBasedOnConfiguration() orElse listFilesBasedOnGit() getOrElse listAllFiles())
      .evalFilterNot(Files[IO].isDirectory)
      .evalFilter(Files[IO].exists)

  /** List all files based on `git ls-files` command.
    */
  private def listFilesBasedOnGit(): Option[Stream[IO, Path]] =
    processRunner(
      Command("git ls-files", "--others --exclude-standard --cached"),
      config.baseDir
    ).toOption
      .map(_.map(config.baseDir / _).distinct)
      .map(Stream.emits)

  /** List all files from the base directory specified in the Stryker4jvm basedir config key.
    */
  private def listAllFiles(): Stream[IO, Path] = {
    log.warn("No 'files' specified and not a git repository.")
    log.warn("Falling back to copying everything except the 'target/' folder(s)")

    Files[IO].walk(config.baseDir)
  }

  /** List all files based on the 'files' configuration key from stryker4jvm.conf.
    */
  private def listFilesBasedOnConfiguration(): Option[Stream[IO, Path]] =
    if (config.files.isEmpty) None else Some(glob(config.baseDir, config.files))

}
