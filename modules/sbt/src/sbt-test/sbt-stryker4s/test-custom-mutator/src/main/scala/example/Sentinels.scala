package example

/** Values that [[Calculator]] uses but never exposes to its callers.
  *
  * Like [[Deadlines]], these live outside `Calculator.scala` on purpose. Only that file is listed in `strykerMutate`,
  * and a literal whose value cannot be observed from outside the method produces an *equivalent* mutant: the source
  * changes but the behaviour does not, so no test can ever kill it and the score is depressed forever.
  *
  * Both values here are unobservable by construction — one is discarded by `*>`, the other is recovered by `.orElse` —
  * so moving them out is the honest fix. Contrast the exception messages that `Calculator` does surface to callers,
  * which stay inline and are pinned by assertions in `CalculatorTest`.
  */
object Sentinels {

  /** Discarded by `*>` in `auditedOrderTotal`; only its type matters, so that swapping `*>` for `<*` still compiles. */
  val discardedAudit: Int = 1

  /** Recovered by `.orElse` in `orderTotalWithFallback`, so it never reaches a caller. */
  val primaryUnavailable: String = "primary unavailable"
}
