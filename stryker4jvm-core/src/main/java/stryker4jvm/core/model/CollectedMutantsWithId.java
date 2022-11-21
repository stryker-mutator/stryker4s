package stryker4jvm.core.model;

import mutationtesting.MutantResult;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class CollectedMutantsWithId<T extends AST> {
    public final List<MutantResult> mutantResults;
    public final Map<T, List<MutantWithId<T>>> mutations;

    public CollectedMutantsWithId(List<MutantResult> mutantResults, Map<T, List<MutantWithId<T>>> mutations) {
        this.mutantResults = mutantResults;
        this.mutations = mutations;
    }

    public CollectedMutantsWithId() {
        this(new LinkedList<>(), new HashMap<>());
    }
}
