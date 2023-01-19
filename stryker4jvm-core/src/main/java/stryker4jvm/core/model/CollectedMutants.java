package stryker4jvm.core.model;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Container that indicates the collected mutants by a {@link Collector}.
 * @param <T> The AST structure used by the collector.
 */
public class CollectedMutants<T extends AST> {
    /**
     * List containing only those mutations that were ignored, either by configuration constraints or
     * other issues.
     */
    public final List<IgnoredMutation<T>> ignoredMutations;
    /**
     * The actual mutations found.
     * This is a map which maps the original non-mutated AST to a list of acceptable mutations.
     */
    public final Map<T, List<MutatedCode<T>>> mutations;

    public CollectedMutants(List<IgnoredMutation<T>> ignoredMutations, Map<T, List<MutatedCode<T>>> mutations) {
        this.ignoredMutations = ignoredMutations;
        this.mutations = mutations;
    }

    public CollectedMutants() {
        ignoredMutations = new LinkedList<>();
        mutations = new HashMap<>();
    }

    public static final class IgnoredMutation<T> {
        /**
         * Mutated code that was ignored
         */
        public final MutatedCode<T> mutatedCode;
        /**
         * The reason why this mutation was ignored
         */
        public final IgnoredMutationReason reason;

        public IgnoredMutation(MutatedCode<T> mutatedCode, IgnoredMutationReason reason) {
            this.mutatedCode = mutatedCode;
            this.reason = reason;
        }
    }
}
