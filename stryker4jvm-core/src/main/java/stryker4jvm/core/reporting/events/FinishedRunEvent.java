package stryker4jvm.core.reporting.events;

import mutationtesting.MetricsResult;
import mutationtesting.MutationTestResult;
import scala.concurrent.duration.FiniteDuration;

import java.nio.file.Path;

public class FinishedRunEvent<C> {
    public final MutationTestResult<C> report;
    public final MetricsResult metrics;
    public final FiniteDuration duration;
    public final Path reportsLocation;

    public FinishedRunEvent(MutationTestResult<C> report, MetricsResult metrics, FiniteDuration duration, Path reportsLocation) {
        this.report = report;
        this.metrics = metrics;
        this.duration = duration;
        this.reportsLocation = reportsLocation;
    }
}
