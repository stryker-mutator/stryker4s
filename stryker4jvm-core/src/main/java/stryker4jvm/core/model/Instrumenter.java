package stryker4jvm.core.model;

import stryker4jvm.model.MutantWithId;

import java.util.List;

public interface Instrumenter<T extends AST> {
    T instrument(T source, List<MutantWithId<T>> mutations);
}
