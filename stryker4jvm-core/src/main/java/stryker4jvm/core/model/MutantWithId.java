package stryker4jvm.core.model;

import stryker4jvm.model.MutatedCode;

public final class MutantWithId<T> {
    public final int id;
    public final MutatedCode<T> mutatedCode;

    public MutantWithId(int id, MutatedCode<T> mutatedCode) {
        this.id = id;
        this.mutatedCode = mutatedCode;
    }
}
