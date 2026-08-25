package example

import scala.concurrent.duration.*

/** Deadlines used by [[Calculator]].
  *
  * These live outside `Calculator.scala` on purpose: only that file is listed in `strykerMutate`,
  * so keeping the durations here stops `NumericLiteralMutator` from producing equivalent mutants
  * (nudging a 200ms sleep to 201ms changes nothing observable) that would otherwise survive and
  * depress the mutation score for no useful reason.
  */
object Deadlines {
  /** Comfortably longer than [[limit]], so the deadline always fires. */
  val slowWork: FiniteDuration = 200.millis

  val limit: FiniteDuration = 10.millis
}
