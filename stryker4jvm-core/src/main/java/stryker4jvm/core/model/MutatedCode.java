package stryker4jvm.core.model;

public final class MutatedCode<T> {
    public final T mutatedStatement;
    public final MutantMetaData metaData;

    public MutatedCode(T mutatedStatement, MutantMetaData metaData) {
        this.mutatedStatement = mutatedStatement;
        this.metaData = metaData;
    }
}
