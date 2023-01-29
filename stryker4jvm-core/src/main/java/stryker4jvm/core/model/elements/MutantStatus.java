package stryker4jvm.core.model.elements;

public enum MutantStatus {
  /** Indicates that the mutant was killed. */
  Killed,
  /** Indicates that the mutant has survived. */
  Survived,
  /** Indicates that the mutant has not been covered at runtime. */
  NoCoverage,
  /** Indicates that the mutant caused a timeout during runtime. */
  Timeout,
  /** Indicates that the mutant could not be compiled at all. */
  CompileError,
  /** Indicates that the mutant caused a runtime exception. */
  RuntimeError,
  /** Indicates that the mutant was ignored entirely, most likely due to user configuration. */
  Ignored
}
