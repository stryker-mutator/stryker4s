package stryker4jvm.core.model.elements;

public class Location {
    public final Position start;
    public final Position end;

    public Location(Position start, Position end) {
        this.start = start;
        this.end = end;
    }
}
