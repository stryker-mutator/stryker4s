package stryker4s.model

import java.nio.file.Path

import scala.concurrent.duration.Duration

case class MutantRunResults(results: Iterable[MutantRunResult], mutationScore: Double, duration: Duration) {
  @transient val timestamp: Long = System.currentTimeMillis()
}

/** The base result type of a mutant run.
  * Extends Product with Serializable to clean up the type signature, as all subtypes are case classes
  */
sealed trait MutantRunResult extends Product with Serializable {
  val mutant: Mutant
  val fileSubPath: Path
}

sealed trait Detected extends MutantRunResult

sealed trait Undetected extends MutantRunResult

case class Killed(mutant: Mutant, fileSubPath: Path) extends Detected

case class TimedOut(mutant: Mutant, fileSubPath: Path) extends Detected

case class Survived(mutant: Mutant, fileSubPath: Path) extends Undetected

/** TODO: Not supported yet
  */
case class NoCoverage(mutant: Mutant, fileSubPath: Path) extends Undetected

/** TODO: Not supported yet
  */
case class Error(mutant: Mutant, fileSubPath: Path) extends MutantRunResult
