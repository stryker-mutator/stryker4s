package stryker4jvm.core.model;

public enum InstrumenterOptions {
  /**
   * The active mutation can be found under System properties with {@link #KEY}. The value may be
   * updated dynamically at runtime.
   */
  SysProp,
  /**
   * The active mutation can be found under System environment variables with {@link #KEY}. This is
   * a constant value that cannot change during runtime.
   */
  EnvVar,
  /**
   * The active mutation can be found in scala class injected at runtime. At the moment only
   * available for scala-mutator. Other mutators should throw a {@link
   * stryker4jvm.core.exception.UnsupportedInstrumenterOptionsException}.
   */
  TestRunner;
  /** The key for system properties and environment variables */
  public static final String KEY = "ACTIVE_MUTATION";
}
