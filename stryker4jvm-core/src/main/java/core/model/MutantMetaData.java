package core.model;

import mutationtesting.Location;

public record MutantMetaData(
        String original,
        String replacement,
        String mutatorName,
        Location location) {

    public String showLocation() {
        return String.format("%d:%d to %d:%d",
                location.start().line(), location.start().column(),
                location.end().line(), location.end().column());
    }
}
