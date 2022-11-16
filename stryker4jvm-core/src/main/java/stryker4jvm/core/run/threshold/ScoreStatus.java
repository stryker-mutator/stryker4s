package stryker4jvm.core.run.threshold;

public enum ScoreStatus {
    Success, Warning, Danger, Error;

    public ScoreStatus determineScoreStatus(Thresholds thresholds, double score) {
        if (score < thresholds.error) return Error;
        if (score < thresholds.low) return Danger;
        if (score < thresholds.high) return Warning;
        return Success;
    }
}
