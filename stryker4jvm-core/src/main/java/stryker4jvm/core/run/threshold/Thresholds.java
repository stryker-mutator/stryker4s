package stryker4jvm.core.run.threshold;

public class Thresholds {
    public final int high;
    public final int low;
    public final int error;

    public Thresholds() {
        this(80, 60, 0);
    }

    public Thresholds(int high, int low, int error) {
        this.high = high;
        this.low = low;
        this.error = error;
    }
}
