package stryker4jvm.core.model.elements;

public enum MutantStatus {
    Killed, Survived, NoCoverage, Timeout,
    CompileError, RuntimeError, Ignored
}
