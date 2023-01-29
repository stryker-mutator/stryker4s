package stryker4jvm.core.model;

import java.nio.file.Path;

import stryker4jvm.core.exception.Stryker4jvmException;

public interface Parser<T extends AST> {
  /**
   * Parses the file found at provided path to an appropriate AST.
   *
   * @param p The path to the file
   * @return An appropriate AST
   * @throws Stryker4jvmException when the file could not be parsed into an AST.
   */
  T parse(Path p) throws Stryker4jvmException;
}
