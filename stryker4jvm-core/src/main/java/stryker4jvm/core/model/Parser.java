package stryker4jvm.core.model;

import java.nio.file.Path;

import stryker4jvm.core.config.LanguageMutatorConfig;
import stryker4jvm.core.exception.Stryker4jvmException;

public interface Parser<T extends AST> {
    /**
     * Parses the file found at provided path to an appropriate AST.
     * @param p The path to the file
     * @return An appropriate AST
     * @throws IOException when the file could not be parsed into an AST. todo: IOException should be replaced with a parse exception
     */
    T parse(Path p) throws Stryker4jvmException;
}
