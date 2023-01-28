package stryker4jvm.core.model;

import stryker4jvm.core.model.elements.Location;

/** Data class that describes the original and mutated code with strings */
public final class MutantMetaData {

  public final String original;
  public final String replacement;
  public final String mutatorName;
  public final Location location;

  public MutantMetaData(
      String original, String replacement, String mutatorName, Location location) {
    this.original = original;
    this.replacement = replacement;
    this.mutatorName = mutatorName;
    this.location = location;
  }

  public String showLocation() {
    return String.format(
        "%d:%d to %d:%d",
        location.start.line, location.start.column, location.end.line, location.end.column);
  }
}
