package stryker4s.run
import java.nio.file.{Files, Path}

import better.files.File
import grizzled.slf4j.Logging
import stryker4s.config.Config
import stryker4s.extensions.PathExtensions._
import stryker4s.extensions.score.MutationScoreCalculator
import stryker4s.model._
import stryker4s.mutants.findmutants.SourceCollector
import stryker4s.run.process.ProcessRunner

import scala.annotation.tailrec
import scala.concurrent.duration.{Duration, MILLISECONDS}

abstract class MutantRunner(process: ProcessRunner)(implicit config: Config)
    extends MutationScoreCalculator
    with Logging {

  private val startTime = System.currentTimeMillis()

  def apply(mutatedFiles: Iterable[MutatedFile],
            sourceCollector: SourceCollector): MutantRunResults = {
    val tmpDir = prepareEnv(mutatedFiles, sourceCollector)

    val runResults = runMutantsRec(mutatedFiles, tmpDir)

    val duration = Duration(System.currentTimeMillis() - startTime, MILLISECONDS)
    val detected = runResults collect { case d: Detected => d }

    MutantRunResults(runResults, calculateMutationScore(runResults.size, detected.size), duration)
  }

  private def prepareEnv(mutatedFiles: Iterable[MutatedFile], sourceCollector: SourceCollector): File = {

    val targetFolder = config.baseDir / "target"
    targetFolder.createDirectoryIfNotExists()

    val files = sourceCollector.filesToCopy(process)

    val tmpDir = File.newTemporaryDirectory("stryker4s-", Option(targetFolder))
    debug("Using temp directory: " + tmpDir)

    files foreach { file =>

      val subPath = file.path.relative
      val filePath = tmpDir / subPath.toString

      if(file.isDirectory) {
        filePath.createDirectoryIfNotExists(createParents = true)
      } else {
        filePath.createFileIfNotExists(createParents = true)
      }

      file.copyTo(filePath, overwrite = true)
    }

    // Overwrite files to mutated files
    mutatedFiles foreach { mutatedFile =>
      val subPath = mutatedFile.fileOriginPath.relative
      val filePath = tmpDir / subPath.toString
      filePath.overwrite(mutatedFile.tree.syntax)
    }
    tmpDir
  }

  private def runMutantsRec(mutatedFiles: Iterable[MutatedFile], tmpDir: File): Iterable[MutantRunResult] = {
    val totalMutants = mutatedFiles.flatMap(_.mutants).size

    @tailrec
    def mutateNext(mutatedFilesIterator: Iterable[MutatedFile], runResultList: List[MutantRunResult]): Iterable[MutantRunResult] ={
      mutatedFilesIterator match {
        case Nil => runResultList
        case mutatedFile :: tail => {
          val subPath = mutatedFile.fileOriginPath.relative

          @tailrec
          def runNextMutant(mutantIterator: Seq[Mutant], mutantResultList: List[MutantRunResult]): List[MutantRunResult] = {
            mutantIterator match {
              case Nil => mutantResultList
              case mutant :: (tail: List[Mutant]) => {
                val result = runMutant(mutant, tmpDir, subPath)
                val id = mutant.id + 1
                info(s"Finished mutation run $id/$totalMutants (${((id / totalMutants.toDouble) * 100).round}%)")

                runNextMutant(tail, result :: mutantResultList)
              }
            }
          }

          val newResults = runNextMutant(mutatedFile.mutants, List[MutantRunResult]())
          val newList = newResults ++ runResultList
          mutateNext(tail, newList)
        }
      }
    }
    mutateNext(mutatedFiles, List[MutantRunResult]())
  }

  private def runMutants(mutatedFiles: Iterable[MutatedFile],
                         tmpDir: File): Iterable[MutantRunResult] = {
    // TODO: Decide if we want this or the tailrec function above
    val totalMutants = mutatedFiles.flatMap(_.mutants).size

    for {
      mutatedFile <- mutatedFiles
      subPath = mutatedFile.fileOriginPath.relative
      mutant <- mutatedFile.mutants
    } yield {
      val result = runMutant(mutant, tmpDir, subPath)
      val id = mutant.id + 1
      info(s"Finished mutation run $id/$totalMutants (${((id / totalMutants.toDouble) * 100).round}%)")
      result
    }
  }

  def runMutant(mutant: Mutant, workingDir: File, subPath: Path): MutantRunResult

}
