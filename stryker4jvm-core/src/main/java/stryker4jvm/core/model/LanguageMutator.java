package stryker4jvm.core.model;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import stryker4jvm.core.config.LanguageMutatorConfig;

public class LanguageMutator<T extends AST> {
    private final Parser<T> parser;
    private final Collector<T> collector;
    private final Instrumenter<T> instrumenter;
    private LanguageMutatorConfig languageConfig;

    public LanguageMutator(Parser<T> parser, Collector<T> collector, Instrumenter<T> instrumenter) {
        this.parser = parser;
        this.collector = collector;
        this.instrumenter = instrumenter;
    }

    public void setLanguageConfig(LanguageMutatorConfig config) {
        this.languageConfig = config;
    }

    public T parse(Path p) throws IOException {
        return parser.parse(p, languageConfig);
    }

    public CollectedMutants<T> collect(AST tree) {
        return collector.collect((T) tree, languageConfig);
    }

    public T instrument(AST source, Map<AST, List<MutantWithId<AST>>> mutations) {
        return instrumenter.instrument((T) source,
            (Map<T, List<MutantWithId<T>>>) (Map<T, ?>) mutations,
            languageConfig
        );
    }
}
