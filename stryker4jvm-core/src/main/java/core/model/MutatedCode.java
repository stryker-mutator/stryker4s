package core.model;

public record MutatedCode<T>(T mutatedStatement, MutantMetaData metaData) {
}
