package stryker4jvm.core.model;

import java.io.IOException;
import java.nio.file.Path;

import stryker4jvm.core.config.LanguageMutatorConfig;

public interface Parser<T extends AST> {
    T parse(Path p, LanguageMutatorConfig config) throws IOException;
}
