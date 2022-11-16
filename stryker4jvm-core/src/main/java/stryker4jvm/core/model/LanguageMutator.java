package stryker4jvm.core.model;

public abstract class LanguageMutator<T extends AST> {
    private final Parser<T> parser;
    private final Collector<T> collector;
    private final Instrumenter<T> instrumenter;

    public LanguageMutator(Parser<T> parser, Collector<T> collector, Instrumenter<T> instrumenter) {
        this.parser = parser;
        this.collector = collector;
        this.instrumenter = instrumenter;
    }
}
