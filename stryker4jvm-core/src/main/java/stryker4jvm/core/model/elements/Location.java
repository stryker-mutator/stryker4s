package stryker4jvm.core.model.elements;

/** Span of positions in text of a child of an AST. */
public class Location {
  public final Position start;
  public final Position end;

  public Location(Position start, Position end) {
    this.start = start;
    this.end = end;
  }
}
