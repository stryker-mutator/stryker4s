package stryker4jvm.core.model;

/**
 * It is required to override hashCode and equals in order for the object to be usable in maps
 * accordingly
 */
public abstract class AST {
    public abstract String syntax();
    public abstract int hashCode();
    public abstract boolean equals(Object obj);
}
