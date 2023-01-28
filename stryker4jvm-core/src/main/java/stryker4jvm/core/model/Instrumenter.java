package stryker4jvm.core.model;

import java.util.List;
import java.util.Map;

public interface Instrumenter<T extends AST> {
  /**
   * Instruments the mutations into the source.
   *
   * @param source The original source without mutations.
   * @param mutations The mutations to instrument into the source.
   * @return A new AST instance which has the mutations included in its structure.
   */
  T instrument(T source, Map<T, List<MutantWithId<T>>> mutations);
}
