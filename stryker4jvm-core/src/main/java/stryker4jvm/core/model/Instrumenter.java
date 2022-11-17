package stryker4jvm.core.model;

import java.util.List;
import java.util.Map;

public interface Instrumenter<T extends AST> {
    T instrument(T source, Map<T, List<MutantWithId<T>>> mutations);
}
