package stryker4jvm.core.model;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import stryker4jvm.core.exception.Stryker4jvmException;

/**
 * Class that encapsulates the three components of a mutator for a specific (programming) language.
 *
 * @param <T> The language-specific AST.
 */
public class LanguageMutator<T extends AST> {
  private final Parser<T> parser;
  private final Collector<T> collector;
  private final Instrumenter<T> instrumenter;

  public LanguageMutator(Parser<T> parser, Collector<T> collector, Instrumenter<T> instrumenter) {
    this.parser = parser;
    this.collector = collector;
    this.instrumenter = instrumenter;
  }

  public T parse(Path p) throws Stryker4jvmException {
    return parser.parse(p);
  }

  public CollectedMutants<T> collect(AST tree) {
    return collector.collect((T) tree);
  }

  public T instrument(AST source, Map<AST, List<MutantWithId<AST>>> mutations) {
    return instrumenter.instrument(
        (T) source, (Map<T, List<MutantWithId<T>>>) (Map<T, ?>) mutations);
  }
}
