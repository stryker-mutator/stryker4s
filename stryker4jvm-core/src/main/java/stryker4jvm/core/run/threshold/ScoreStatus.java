package stryker4jvm.core.run.threshold;

public enum ScoreStatus {
    SUCCESS, WARNING, DANGER, ERROR;

    public ScoreStatus determineScoreStatus(Thresholds thresholds, double score) {
        if (score < thresholds.error) return ERROR;
        if (score < thresholds.low) return DANGER;
        if (score < thresholds.high) return WARNING;
        return SUCCESS;
    }
}
