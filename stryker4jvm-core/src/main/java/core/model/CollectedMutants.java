package core.model;

import stryker4jvm.model.IgnoredMutationReason;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class CollectedMutants<T extends AST> {
    public final List<IgnoredMutation<T>> ignoredMutations;
    public final Map<T, List<MutatedCode<T>>> mutations;

    public CollectedMutants(List<IgnoredMutation<T>> ignoredMutations, Map<T, List<MutatedCode<T>>> mutations) {
        this.ignoredMutations = ignoredMutations;
        this.mutations = mutations;
    }

    public CollectedMutants() {
        ignoredMutations = new LinkedList<>();
        mutations = new HashMap<>();
    }

    public record IgnoredMutation<T>(MutatedCode<T> mutatedCode, IgnoredMutationReason reason) {

    }
}
