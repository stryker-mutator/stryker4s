package stryker4jvm.core.model;

import stryker4jvm.core.model.elements.MutantResult;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class CollectedMutantsWithId<T extends AST> {
    // todo: should this even be here? This is probably a remnant from the original stryker4s MutantWithId
    public final List<MutantResult> mutantResults;
    /**
     * The actual mutations found.
     * This is a map which maps the original non-mutated AST to a list of accepted mutations.
     * The mutants are accompanied by a unique identifier.
     */
    public final Map<T, List<MutantWithId<T>>> mutations;

    public CollectedMutantsWithId(List<MutantResult> mutantResults, Map<T, List<MutantWithId<T>>> mutations) {
        this.mutantResults = mutantResults;
        this.mutations = mutations;
    }

    public CollectedMutantsWithId() {
        this(new LinkedList<>(), new HashMap<>());
    }
}
