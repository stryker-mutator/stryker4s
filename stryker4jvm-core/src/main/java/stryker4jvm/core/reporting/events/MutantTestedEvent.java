package stryker4jvm.core.reporting.events;

public final class MutantTestedEvent {
    public final int totalMutants;

    public MutantTestedEvent(int totalMutants) {
        this.totalMutants = totalMutants;
    }
}
