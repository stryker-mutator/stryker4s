package stryker4jvm.core.model;

import java.util.List;
import java.util.Map;

import stryker4jvm.core.config.LanguageMutatorConfig;

public interface Instrumenter<T extends AST> {
    /**
     * Instruments the mutations into the source.
     * @param source The original source without mutations.
     * @param mutations The mutations to instrument into the source.
     * @return A new AST instance which has the mutations included in its structure.
     */
    T instrument(T source, Map<T, List<MutantWithId<T>>> mutations,
        LanguageMutatorConfig config);

    default void f() {
        System.getProperty("");
        java.lang.System.getenv("");
    }

    enum InstrumenterOptions {
        /**
         * The active mutation can be found under System properties with {@link #KEY}.
         */
        SysProp,
        /**
         * The active mutation can be found under System environment variables with {@link #KEY}
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
}
