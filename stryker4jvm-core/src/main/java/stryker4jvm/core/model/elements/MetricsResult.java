package stryker4jvm.core.model.elements;


@Deprecated(since="1.0", forRemoval = true)
public class MetricsResult {
    public final int killed;
    public final int survived;
    public final int timeout;
    public final int noCoverage;
    public final int compileErrors;
    public final int runtimeErrors;
    public final int ignored;

    public MetricsResult(int killed, int survived, int timeout, int noCoverage, int compileErrors, int runtimeErrors, int ignored) {
        this.killed = killed;
        this.survived = survived;
        this.timeout = timeout;
        this.noCoverage = noCoverage;
        this.compileErrors = compileErrors;
        this.runtimeErrors = runtimeErrors;
        this.ignored = ignored;
    }

    public int totalDetected() {
        return killed + timeout;
    }

    public int totalUndetected() {
        return survived + noCoverage;
    }

    public int totalCovered() {
        return totalDetected() + survived;
    }

    public int totalValid() {
        return totalDetected() + totalUndetected();
    }

    public int totalInvalid() {
        return runtimeErrors + compileErrors;
    }

    public int totalMutants() {
        return totalValid() + totalInvalid() + ignored;
    }

    public double mutationScore() {
        return (totalDetected() / (double)totalValid()) * 100;
    }

    public double mutationScoreBasedOnCoveredCode() {
        return (totalDetected() / (double)totalCovered()) * 100;
    }
}
