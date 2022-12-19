package stryker4jvm.core.model;

import java.util.List;
import java.util.Map;

import stryker4jvm.core.config.LanguageMutatorConfig;

public interface Instrumenter<T extends AST> {
    T instrument(T source, Map<T, List<MutantWithId<T>>> mutations,
        LanguageMutatorConfig config);
}
