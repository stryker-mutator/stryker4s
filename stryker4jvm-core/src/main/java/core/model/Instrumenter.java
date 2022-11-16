package core.model;

import java.util.List;

public interface Instrumenter<T extends AST> {
    T instrument(T source, List<MutantWithId<T>> mutations);
}
