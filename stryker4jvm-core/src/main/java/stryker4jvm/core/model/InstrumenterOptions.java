package stryker4jvm.core.model;

public enum InstrumenterOptions {
    /**
     * The active mutation can be found under System properties with {@link #KEY}. The value may be updated
     * dynamically at runtime.
     */
    SysProp,
    /**
     * The active mutation can be found under System environment variables with {@link #KEY}. This is a constant
     * value that cannot change during runtime.
     */
    EnvVar,
    /**
     * Unclear what this does exactly. I believe that this allows the instrumenter to refer to a static
     * field of a certain class that is dynamically being updated between runs. Additionally, it has some kind
     * of conditional filter which is unclear why it has that.
     */
    TestRunner;
    /**
     * The key for system properties and environment variables
     */
    public static final String KEY = "ACTIVE_MUTATION";
}