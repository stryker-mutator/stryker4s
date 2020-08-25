package stryker4s.model

import java.nio.file.Path

/** The base result of a mutant run.
  */
sealed trait MutantRunResult {
  def mutant: Mutant
  def fileSubPath: Path
  def description: Option[String]
}

sealed trait Detected extends MutantRunResult

sealed trait Undetected extends MutantRunResult

final case class Killed(mutant: Mutant, fileSubPath: Path, description: Option[String] = None) extends Detected

final case class TimedOut(mutant: Mutant, fileSubPath: Path, description: Option[String] = None) extends Detected

final case class Survived(mutant: Mutant, fileSubPath: Path, description: Option[String] = None) extends Undetected

/** TODO: Not supported yet
  */
final case class NoCoverage(mutant: Mutant, fileSubPath: Path, description: Option[String] = None) extends Undetected

final case class Error(mutant: Mutant, fileSubPath: Path, description: Option[String] = None) extends MutantRunResult
