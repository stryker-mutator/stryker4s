package stryker4jvm.core.model;

import stryker4jvm.core.config.LanguageMutatorConfig;

public interface Collector<T extends AST> {
    CollectedMutants<T> collect(T tree, LanguageMutatorConfig config);
}
