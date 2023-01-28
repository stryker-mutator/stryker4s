package stryker4jvm.core.model;

public interface Collector<T extends AST> {
  /**
   * Collects all possible mutations from the provided source tree.
   *
   * @param tree The source
   * @return A {@link CollectedMutants collection} of mutants.
   */
  CollectedMutants<T> collect(T tree);
}
