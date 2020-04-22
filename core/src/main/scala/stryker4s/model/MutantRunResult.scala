package stryker4s.model

import java.nio.file.Path

/** The base result type of a mutant run.
  * Extends Product with Serializable to clean up the type signature, as all subtypes are case classes
  */
sealed trait MutantRunResult extends Product with Serializable {
  def mutant: Mutant
  def fileSubPath: Path
}

sealed trait Detected extends MutantRunResult

sealed trait Undetected extends MutantRunResult

final case class Killed(mutant: Mutant, fileSubPath: Path) extends Detected

final case class TimedOut(mutant: Mutant, fileSubPath: Path) extends Detected

final case class Survived(mutant: Mutant, fileSubPath: Path) extends Undetected

/** TODO: Not supported yet
  */
final case class NoCoverage(mutant: Mutant, fileSubPath: Path) extends Undetected

/** TODO: Not supported yet
  */
final case class Error(mutant: Mutant, fileSubPath: Path) extends MutantRunResult
