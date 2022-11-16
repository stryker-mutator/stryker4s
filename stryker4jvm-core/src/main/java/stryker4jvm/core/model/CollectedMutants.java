package stryker4jvm.core.model;

import stryker4jvm.model.IgnoredMutationReason;
import stryker4jvm.model.MutatedCode;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class CollectedMutants<T extends AST> {
    public final List<IgnoredMutation<T>> ignoredMutations;
    public final Map<T, List<stryker4jvm.model.MutatedCode<T>>> mutations;

    public CollectedMutants(List<IgnoredMutation<T>> ignoredMutations, Map<T, List<stryker4jvm.model.MutatedCode<T>>> mutations) {
        this.ignoredMutations = ignoredMutations;
        this.mutations = mutations;
    }

    public CollectedMutants() {
        ignoredMutations = new LinkedList<>();
        mutations = new HashMap<>();
    }

    public static final class IgnoredMutation<T> {
        public final MutatedCode<T> mutatedCode;
        public final IgnoredMutationReason reason;

        public IgnoredMutation(MutatedCode<T> mutatedCode, IgnoredMutationReason reason) {
            this.mutatedCode = mutatedCode;
            this.reason = reason;
        }
    }
}
