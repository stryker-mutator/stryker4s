package stryker4jvm.core.model;

/**
 * Data class that contains the mutated code for a mutation.
 * @param <T> The structure of a mutated statement
 */
public final class MutatedCode<T> {
    /**
     * The mutated statement.
     * That is, mutated statement is the structure that will replace the original non-mutated structure.
     */
    public final T mutatedStatement;
    public final MutantMetaData metaData;

    public MutatedCode(T mutatedStatement, MutantMetaData metaData) {
        this.mutatedStatement = mutatedStatement;
        this.metaData = metaData;
    }
}
