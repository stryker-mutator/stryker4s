package core.model;

public interface Collector<T extends AST> {
    void collect();
}
