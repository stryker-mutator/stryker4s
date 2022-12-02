package stryker4jvm.core.model.elements;

public class Position {
    public final int line;
    public final int column;

    public Position(int line, int column) {
        this.line = line;
        this.column = column;
    }
}
